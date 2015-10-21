/*
 *  __   ___   ____  ____  ___   ___
 * ( (` | |_) | |_  | |_  | | \ | | \
 * _)_) |_|   |_|__ |_|__ |_|_/ |_|_/
 *
 * SPEEDD project (www.speedd-project.eu)
 * Machine Learning module
 *
 * Copyright (c) Complex Event Recognition Group (cer.iit.demokritos.gr)
 *
 * NCSR Demokritos
 * Institute of Informatics and Telecommunications
 * Software and Knowledge Engineering Laboratory
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with program. If not, see <http://www.gnu.org/licenses/>.
 */

package org.speedd.ml.learners

import java.io._
import auxlib.log.Logging
import lomrf.logic._
import lomrf.mln.model.{EvidenceBuilder, KB}
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.{DataFrame, SQLContext}
import org.apache.spark.sql.functions._
import com.datastax.spark.connector._
import org.speedd.ml.model.CNRS
import org.speedd.ml.model.CNRS.{Location, Annotation, RawInput}
import scala.collection.breakOut
import java.util.Arrays.binarySearch
import scala.language.implicitConversions
import scala.util.{Failure, Success}
import org.speedd.ml.util.DataFrameOps._
import org.speedd.ml.util.logic._

object CNRSWeightsEstimator extends WeightEstimator with Logging {

  private type RAW_INPUT_TUPLE = (Long, String, Int, Option[Double], Option[Double], Option[Double])
  private type ANNOTATION_TUPLE = (Int, Int, String)

  private val queryPredicates = Set[AtomSignature](AtomSignature("HoldsAt", 2))
  private val hiddenPredicates = Set[AtomSignature](AtomSignature("InitiatedAt", 2), AtomSignature("TerminatedAt", 2))
  private val convertFunctionsToPredicates = false

  private val inputColumnNames = Seq("loc_id", "lane", "timestamp", "avg_speed", "occupancy", "vehicles")

  private val tempEvidenceTableName = "TEMP_EVIDENCE"
  private val tempLocationTableName = "TEMP_LOCATION"

  private val domainAliases = Map("start_loc" -> "loc_id", "end_loc" -> "loc_id")

  private val occLevels = Array(0.0, 25.0, 50.0, 75.0)
  private val speedLevels = Array(0.0, 50.0, 90.0, 100.0, 130.0)
  private val vehicleLevels = Array(0.0, 4.0, 8.0, 16.0, 32.0)


  private val domain2udf = Map(
    "occupancy" -> udf {
      (x: Double) =>
        val pos = binarySearch(occLevels, x)
        val bin = if (pos < 0) -pos - 2 else pos
        "O" + bin
    },

    "avg_speed" -> udf {
      (x: Double) =>
        val pos = binarySearch(speedLevels, x)
        val bin = if (pos < 0) -pos - 2 else pos
        "S" + bin
    },

    "vehicles" -> udf {
      (x: Double) =>
        val pos = binarySearch(vehicleLevels, x)
        val bin = if (pos < 0) -pos - 2 else pos
        "V" + bin
    }
  )


  implicit def string2ColumnNames(names: Seq[String]): Seq[ColumnRef] = {
    names.map(name => new ColumnName(name))
  }


  private def collectDomains(source: DataFrame*): Map[String, RDD[String]] = {

    def domainsOf(df: DataFrame): Map[String, DataFrame] = {
      df.columns.map { colName =>
        domainAliases.getOrElse(colName, colName) -> df.select(colName).filter(df(colName).isNotNull).distinct()
      }(breakOut)
    }

    def mergeDomains(m1: Map[String, DataFrame], m2: Map[String, DataFrame]): Map[String, DataFrame] = m1 ++ m2.map {
      case (k, v) => m1.get(k) match {
        case Some(d) => k -> d.unionAll(v)
        case None => k -> v
      }
    }

    source.map(domainsOf)
      .aggregate(Map.empty[String, DataFrame])(mergeDomains, mergeDomains)
      .mapValues(_.distinct().map(_.get(0).toString))
  }


  override def learn(startTime: Long, endTime: Long, inputKB: File, outputKB: File,
                     inputSignatures: Set[AtomSignature], targetSignatures: Set[AtomSignature])
                    (implicit sc: SparkContext, sqlContext: SQLContext): Unit = {

    import sqlContext.implicits._

    // ---
    // --- Prepare the KB:
    // ---
    // Attempt to simplify the given KB --- i.e., exploit input data, in order to eliminate existentially
    // quantified variables that may appear in the body part of each definite clause
    info(s"Processing the given input KB '${inputKB.getPath}', in order to make simplifications.")
    val (kb, constantsDomainBuilder) = KB.fromFile(inputKB.getPath)

    // Try to simplify the knowledge base, in order to eliminate existential quantifiers.
    val ruleTransformations = KBSimplifier
      .transform(kb.definiteClauses, inputSignatures, targetSignatures, constantsDomainBuilder.result()) match {
      case Success(result) =>
        // Output all transformations into a log file
        exportTransformations(
          ruleTransformations = result,
          outputPath = inputKB.getPath + s"_$startTime-$endTime.log",
          interval = Some((startTime, endTime))) match {

          case Success(path) => info(s"KB transformations are written into '$path'")

          case Failure(ex) => info(s"Failed to write KB transformations", ex)
        }

        // give the resulting transformations
        result
      case Failure(ex) => fatal(s"Failed to transform the given knowledge base '${inputKB.getPath}'", ex)
    }

    // ---
    // --- Create final predicate schema:
    // ---
    // That is, query predicates, evidence predicates and all derived atoms from rule transformations.
    // Please note that weight learning does not support hidden predicates, therefore we do not include their signatures.
    var finalPredicateSchema = kb.predicateSchema -- hiddenPredicates

    for (transformation <- ruleTransformations)
      finalPredicateSchema ++= transformation.schema


    // make sure that we have all time-points of the specified temporal interval
    val timestampDomain = startTime to endTime
    val timestampsDF = sc.parallelize(timestampDomain).toDF("timestamp").cache()

    // ---
    // --- Load raw data for the specified temporal interval
    // ---
    // The data is loaded from raw input
    info(s"Loading raw data for the temporal interval [$startTime, $endTime]")

    // discretize numeric values such as `occupancy`,`avg_speed`, etc.
    val mappedInputColumns = inputColumnNames.map(colName => domain2udf.get(colName) match {
      case Some(f) => f($"$colName") as colName
      case None => $"$colName"
    })

    val rawInputDF = sc.cassandraTable[RAW_INPUT_TUPLE](CNRS.KEYSPACE, RawInput.tableName)
      .select(inputColumnNames: _*) // select only the desired subset of columns, with the given ordering
      .where("timestamp >= ? AND timestamp <= ?", startTime, endTime) // take the rows of the specified temporal interval
      .toDF(inputColumnNames: _*) // represent the selected rows as a data frame
      .select(mappedInputColumns: _*) // apply conversions, e.g., discretize numeric values such as `occupancy`,`avg_speed`, etc.
      .cache() // try to cache the data frame, because we will parse it multiple times.

    // Represent the selected data as a temporary in-memory table, named as `TEMP_EVIDENCE`.
    rawInputDF.registerTempTable(tempEvidenceTableName)

    // ---
    // --- Load location data for the specified temporal interval
    // ---
    info(s"Loading location data")
    val locationDF = sc.cassandraTable[Location](CNRS.KEYSPACE, Location.tableName)
      .toDF(Location.columnNames: _*)
      .cache()

    // Represent the location data as a temporary in-memory table, named as `TEMP_LOCATION`.
    locationDF.registerTempTable(tempLocationTableName)

    // ---
    // --- Load annotation data for the specified interval
    // ---
    info(s"Loading annotation data for the temporal interval [$startTime, $endTime]")

    // Collect overlapping Annotations with the specified temporal interval [startTime, endTime].
    // Thereafter, flatten the annotations from Annotation instances that represent the rows of (start_ts, end_ts,
    // event_num, description, sens, start_loc, end_loc) to tuples of (ts, event_num, description, sens, start_loc,
    // end_loc). Therefore we convert the Annotations containing intervals (start_ts, end_ts, ...) to multiple
    // instantaneous annotations as tuples of (ts, ...).  The resulting columns of the `overlappingAnnotations` data
    // frame are (ts, event_num, description, sens, start_loc, end_loc)
    /*val overlappingAnnotationsDF = sc.cassandraTable[Annotation](CNRS.KEYSPACE, Annotation.tableName)
      .filter(r => r.startTs <= endTime && r.endTs >= startTime)
      .flatMap(r => (r.startTs to r.endTs).map(t => (t, r.eventNum, r.description, r.sens, r.startLoc, r.endLoc)))
      .toDF("ts", "event_num", "description", "sens", "end_loc", "start_loc")*/


    val l2d = locationDF
      .select("loc_id", "dist", "lane")
      .distinct()
      .cache()

    val annotatedLocations = sc.cassandraTable[Annotation](CNRS.KEYSPACE, Annotation.tableName)
      .filter(r => r.startTs <= endTime && r.endTs >= startTime)
      .toDF(Annotation.columnNames: _*)
      .select($"start_ts", $"end_ts", $"event_num" as "eid", $"description", $"sens", $"start_loc", $"end_loc")
      .cache()

    val annotatedLocationWIds = l2d
      .join(annotatedLocations, $"dist" <= $"start_loc" and $"dist" >= $"end_loc")
      .select("loc_id", "start_ts", "end_ts", "description", "lane")
      .flatMap{ r =>
        val loc_id = r.getLong(0)
        val description = r.getString(3)
        val lane = r.getString(4)
        (r.getInt(1) to r.getInt(2)).map(t => (t, loc_id, description, lane))
      }.toDF("ts", "loc_id", "description", "lane")
      .cache()

    // We take the left outer join between `timestampsDF` and `overlappingAnnotations`, in order to collect the
    // annotation data (including missing instances as nulls) for the specified temporal interval [startTime, endTime].
    val annotationsDF = timestampsDF
      .join(annotatedLocationWIds, $"timestamp" === $"ts", "left_outer")
      .select("timestamp", "loc_id", "description", "lane")
      .cache()

    println("FINAL ANNOTATION DATA!!!!")
    annotationsDF.show()


   // sys.exit()


   /* val annotationsDF = timestampsDF
      .join(overlappingAnnotationsDF, $"timestamp" === $"ts", "left_outer")
      .select($"timestamp", $"start_loc", $"end_loc", $"description")
      //.join(sdistIntervals, $"start_loc" === $"startDist" and $"end_loc" === $"endDist")
      //.join(distIntervals, $"start_loc" <= $"endDist" and $"end_loc" >= $"startDist")
      .distinct()
      .cache()


    annotationsDF.show(10)
    sys.exit()*/


    // ---
    // --- Create constant domains:
    // ---
    //
    val domainMap = collectDomains (
      rawInputDF.select("avg_speed", "occupancy", "vehicles").distinct(),
      locationDF.select("loc_id", "lane").distinct(),
      annotatedLocations.select("description").distinct()
    )

    constantsDomainBuilder ++= ("timestamp", startTime to endTime map(_.toString) )

    for ((domainName, constantsRDD) <- domainMap)
      constantsDomainBuilder ++=(domainName, constantsRDD.collect())

    domainMap.foreach(x => println(s"${x._1} -> (${x._2.count()}})"))


    // ---
    // --- Compute function mappings and their corresponding function mappings
    // ---
    val functionMappings = new Array[RDD[(String, FunctionMapping)]](kb.functionSchema.size)

    for (((signature, (retDomain, argDomains)), index) <- kb.functionSchema.zipWithIndex) {
      val symbol = signature.symbol

      if (domainMap.contains(retDomain))
        fatal(s"Cannot reassign domain '$retDomain' using function mappings.")

      val argDomainsDF = argDomains.map { name =>
        val colName = domainAliases.getOrElse(name, name)
        domainMap.getOrElse(colName, fatal(s"Unknown domain name '$colName'")).toDF(name)
      }

      val products = argDomainsDF.reduceLeft((a, b) => a.repartition(1).join(b.repartition(1)))
        .map(r => (0 until r.length).map(i => Constant(r.get(i).toString)))
        .zipWithUniqueId()
        .map {
          case (constants, uid) =>
            val retConstant = s"r_${index}_$uid"
            retConstant -> FunctionMapping(retConstant, symbol, constants.toVector)
        }.cache()

      val retConstants = products.keys.collect()
      constantsDomainBuilder ++=(retDomain, retConstants)

      info(s"function mapping $symbol(${argDomains.mkString(",")}): ${retConstants.length} groundings")


      functionMappings(index) = products
    }

    val constantsDomain = constantsDomainBuilder.result()

    // ---
    // --- Create a new evidence builder
    // ---
    // With evidence builder we can incrementally create an evidence database for LoMRF
    val trainingDB = EvidenceBuilder(finalPredicateSchema, kb.functionSchema, queryPredicates,
      hiddenPredicates = Set.empty, constantsDomain, convertFunctionsToPredicates)

    // ---
    // --- Store previously computed function mappings
    // ---
    for (fm <- functionMappings) {
      trainingDB.functions ++= fm.values.collect()
      fm.unpersist()
    }

    // ---
    // --- Create auxiliary predicates and give them as evidence facts:
    // ---
    // Compute instances of the auxiliary derived atoms from the raw data in the specified temporal interval.
    info(s"Generating derived events for the temporal interval [$startTime, $endTime]")

    for {transformation <- ruleTransformations
         transformedRule = transformation.transformedRule
         (derivedAtom, sqlConstraint) <- transformation.atomMappings} {

      val terms = transformation.schema(derivedAtom.signature).mkString(",")
      val arity = derivedAtom.arity
      val symbol = derivedAtom.symbol

      val query = s"SELECT $terms FROM $tempEvidenceTableName WHERE $sqlConstraint"

      val instancesDF = sqlContext.sql(query)

      whenDebug {
        debug(s"For the derived atom '${derivedAtom.toText}' we map the query '$query', which gives ${instancesDF.count()} results " +
          s"for the temporal interval [$startTime, $endTime]")
      }

      trainingDB.evidence ++= instancesDF.map { row =>
        // Create a vector of constants from the elements of the current row
        val constants: Vector[Constant] = (0 until arity).map(i => Constant(row.get(i).toString))(breakOut)
        // Create the corresponding evidence atom
        EvidenceAtom.asTrue(symbol, constants)
      }.collect()
    }

    // No need to keep raw input data frame cached in memory
    rawInputDF.unpersist()

    // take the collected function mappings
    val functionMappingsMap = sc.broadcast(trainingDB.functions.result())

    //annotationsDF.filter($"description".isNotNull).distinct().collect().foreach(println)

    // ---
    // --- Create ground-truth predicates (HoldsAt/2)
    // ---
    info(s"Generating annotation predicates for the temporal interval [$startTime, $endTime]")
    val headSignatures = Set(AtomSignature("InitiatedAt", 2), AtomSignature("TerminatedAt",2))

    val fluents = kb.definiteClauses
      .withFilter(wc => headSignatures.contains(wc.clause.head.signature))
      .map(_.clause.head.terms.head)
      .flatMap {
        case TermFunction(symbol, terms, "fluent") => Some((symbol, terms))
        case _ => None
      }

    for ((symbol, terms) <- fluents) {

      val holdsAtInstancesRDD = annotationsDF
        .filter($"description".isNotNull and $"description" === symbol).flatMap { row =>

        val domainMap = Map[String, Constant](
          "timestamp" -> Constant(row.get(0).toString),
          "loc_id" -> Constant(row.get(1).toString),
          "lane" -> Constant(row.get(3).toString)
        )

        val theta = terms.withFilter(_.isVariable)
          .map(_.asInstanceOf[Variable])
          .map(v => v -> domainMap(v.domain))
          .toMap[Term, Term]

        val sterms = terms.map(_.substitute(theta))

        if(sterms.forall(_.isConstant)) {
          val fluentSignature = AtomSignature(symbol, sterms.size)

          val resultingSymbolOpt = functionMappingsMap.value.get(fluentSignature).flatMap(mapper => mapper.get(sterms.map(_.toText)))

          //resultingSymbolOpt

          functionMappingsMap.value.get(fluentSignature) match {
            case Some(mapper) =>
              mapper.get(sterms.map(_.toText)) match {
                case Some(resultingSymbol) =>
                  Some(EvidenceAtom.asTrue("HoldsAt", Vector(Constant(resultingSymbol), Constant(row.get(0).toString))))

                case _ =>
                  error(s"Unknown function mapping for terms '${sterms.map(_.toText).mkString(",")}' for the function with signature '$fluentSignature'")
                  None

              }
            case _ =>
              error(s"Unknown fluent signature '$fluentSignature'")
              None
          }
        }
        else {
          error(s"Found non-constant terms in '${sterms.map(_.toText).mkString(",")}'")
          None
        }

      }

      //holdsAtInstances.take(10).foreach(println)

      val holdsAtInstances = holdsAtInstancesRDD.collect()

      for(atom <- holdsAtInstances) try {
        trainingDB.evidence += atom
      } catch {
        case ex: java.util.NoSuchElementException =>
          val fluent = atom.terms.head
          val timestamp = atom.terms.last

          constantsDomain("fluent").get(fluent.symbol) match{
            case None => error(s"fluent constant ${fluent.symbol} is missing from constants domain}")
            case _ =>
          }

          constantsDomain("timestamp").get(timestamp.symbol) match{
            case None => error(s"timestamp constant ${timestamp.symbol} is missing from constants domain}")
            case _ =>
          }
      }

      //trainingDB.evidence ++= holdsAtInstancesRDD.collect()

    }

    val edb = trainingDB.result().db

    println("ANNOTATION length: " + edb(AtomSignature("HoldsAt", 2)).numberOfTrue)

  }

}
