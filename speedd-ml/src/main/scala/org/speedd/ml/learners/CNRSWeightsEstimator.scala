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
import lomrf.mln.model.{ConstantsDomain, EvidenceBuilder, KB}
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.{DataFrame, SQLContext}
import com.datastax.spark.connector._
import org.speedd.ml.model.CNRS
import org.speedd.ml.model.CNRS.{Location, Annotation, RawInput}
import org.speedd.ml.util.data._
import scala.collection.breakOut
import scala.language.implicitConversions
import scala.reflect.macros.whitebox
import scala.util.{Failure, Success}
import org.speedd.ml.util.logic._

object CNRSWeightsEstimator extends WeightEstimator with Logging {

  private val queryPredicates = Set[AtomSignature](AtomSignature("HoldsAt", 2))
  private val hiddenPredicates = Set[AtomSignature](AtomSignature("InitiatedAt", 2), AtomSignature("TerminatedAt", 2))
  private val convertFunctionsToPredicates = false

  private val tempEvidenceTableName = "TEMP_EVIDENCE"

  private val domainAliases = Map("start_loc" -> "loc_id", "end_loc" -> "loc_id")

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

          case Failure(ex) => error(s"Failed to write KB transformations", ex)
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


    val (timestampsDF, rawInputDF, locationDF, annotationsDF, annotatedLocations) = loadFor(startTime, endTime)


    // ---
    // --- Create constant domains:
    // ---
    //
    val domainMap = symbolsPerColumn(
      rawInputDF.select("avg_speed", "occupancy", "vehicles").distinct(),
      locationDF.select("loc_id", "lane").distinct(),
      annotatedLocations.select("description").distinct()
    )(domainAliases)

    constantsDomainBuilder ++=("timestamp", startTime to endTime map (_.toString))

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
    val headSignatures = Set(AtomSignature("InitiatedAt", 2), AtomSignature("TerminatedAt", 2))

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

        if (sterms.forall(_.isConstant)) {
          val fluentSignature = AtomSignature(symbol, sterms.size)

          //val resultingSymbolOpt = functionMappingsMap.value.get(fluentSignature).flatMap(mapper => mapper.get(sterms.map(_.toText)))

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

      for (atom <- holdsAtInstances) try {
        trainingDB.evidence += atom
      } catch {
        case ex: java.util.NoSuchElementException =>
          val fluent = atom.terms.head
          val timestamp = atom.terms.last

          constantsDomain("fluent").get(fluent.symbol) match {
            case None => error(s"fluent constant ${fluent.symbol} is missing from constants domain}")
            case _ =>
          }

          constantsDomain("timestamp").get(timestamp.symbol) match {
            case None => error(s"timestamp constant ${timestamp.symbol} is missing from constants domain}")
            case _ =>
          }
      }

      //trainingDB.evidence ++= holdsAtInstancesRDD.collect()

    }

    val edb = trainingDB.result().db

    println("ANNOTATION length: " + edb(AtomSignature("HoldsAt", 2)).numberOfTrue)

  }

  private def loadFor(startTime: Long, endTime: Long)(implicit sc: SparkContext, sqlContext: SQLContext) = {
    import sqlContext.implicits._

    // make sure that we have all time-points of the specified temporal interval
    val timestampsDF = sc.parallelize(startTime to endTime).toDF("timestamp").cache()

    // ---
    // --- Load raw data for the specified temporal interval
    // ---
    // The data is loaded from raw input
    info(s"Loading raw data for the temporal interval [$startTime, $endTime]")

    val rawInputDF = RawInput.loadDF
      .where(s"timestamp >= $startTime AND timestamp <= $endTime")
      .cache()

    // Represent the selected data as a temporary in-memory table, named as `TEMP_EVIDENCE`.
    rawInputDF.registerTempTable(tempEvidenceTableName)

    // ---
    // --- Load location data for the specified temporal interval
    // ---
    info(s"Loading location data")
    val locationDF = Location.loadDF.cache()

    // ---
    // --- Load annotation data for the specified interval
    // ---
    info(s"Loading annotation data for the temporal interval [$startTime, $endTime]")

    val annotatedLocations = Annotation.loadDF
      .where(s"start_ts <= $endTime AND end_ts >= $startTime")
      .withColumnRenamed("event_num", "eid")
      .cache()

    val annotationsDF = expandAnnotation(annotatedLocations, locationDF, timestampsDF).cache()

    (timestampsDF, rawInputDF, locationDF, annotationsDF, annotatedLocations)
  }

  private def expandAnnotation(annotatedLocations: DataFrame, locationDF: DataFrame, timestampsDF: DataFrame)
                              (implicit sc: SparkContext, sqlContext: SQLContext): DataFrame = {
    import sqlContext.implicits._

    val annotatedLocationWIds = locationDF.select("loc_id", "dist", "lane").distinct()
      .join(annotatedLocations, $"dist" <= $"start_loc" and $"dist" >= $"end_loc")
      .select("loc_id", "start_ts", "end_ts", "description", "lane")
      .flatMap { r =>
        val loc_id = r.getLong(0)
        val description = r.getString(3)
        val lane = r.getString(4)
        (r.getInt(1) to r.getInt(2)).map(t => (t, loc_id, description, lane))
      }
      .toDF("ts", "loc_id", "description", "lane")

    // We take the left outer join between `timestampsDF` and `overlappingAnnotations`, in order to collect the
    // annotation data (including missing instances as nulls) for the specified temporal interval [startTime, endTime].
    val result = timestampsDF
      .join(annotatedLocationWIds, $"timestamp" === $"ts", "left_outer")
      .select("timestamp", "loc_id", "description", "lane")

    result
  }

}
