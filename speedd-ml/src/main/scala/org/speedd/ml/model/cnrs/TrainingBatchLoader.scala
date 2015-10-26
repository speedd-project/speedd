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

package org.speedd.ml.model.cnrs

import auxlib.log.Logging
import lomrf.logic._
import lomrf.mln.model._
import org.apache.spark.SparkContext
import org.apache.spark.sql.SQLContext
import org.speedd.ml.model.{BatchLoader, TrainingBatch}
import org.speedd.ml.util.logic._

import scala.collection.breakOut
import scala.util.{Failure, Success}

final class TrainingBatchLoader(kb: KB,
                                kbConstants: ConstantsDomain,
                                predicateSchema: PredicateSchema,
                                queryPredicates: Set[AtomSignature],
                                ruleTransformations: Iterable[RuleTransformation],
                                aliases: Map[String, String] = domainAliases ) extends BatchLoader with Logging {


  override def forInterval(startTime: Long, endTime: Long)
                          (implicit sc: SparkContext, sqlContext: SQLContext): TrainingBatch = {

    import sqlContext.implicits._

    val (rawInputDF, locationDF, annotationsDF, annotatedLocations) = loadFor(startTime, endTime)

    //val rawInputDF = _rawInputDF.filter($"lane" !== "")

    // local copy of collections, in order to avoid serializing the entire class
    val _kbConstants = this.kbConstants
    val _predicateSchema = this.predicateSchema
    val _queryPredicates = this.queryPredicates
    val _ruleTransformations = this.ruleTransformations
    val _domainAliases = this.aliases
    val _functionSchema = this.kb.functionSchema
    val _kbDefiniteClauses = this.kb.definiteClauses


    // Represent the selected data as a temporary in-memory table, named as `TEMP_EVIDENCE`.
    rawInputDF.registerTempTable(TrainingBatchLoader.tempEvidenceTableName)

    val domainMap = unionDomains(
      initial = _kbConstants,
      static = List("timestamp" -> (startTime to endTime map (_.toString))),
      df = List(
        rawInputDF.select("avg_speed", "occupancy", "vehicles").distinct(),
        locationDF.select("loc_id", "lane").distinct(),
        annotatedLocations.select("description").distinct()),
      aliases = _domainAliases
    )

    val (functionMappings, generatedDomains) = {
      generateFunctionMappings(_functionSchema, domainMap, _domainAliases) match {
        case Success(result) => result
        case Failure(ex) => fatal("Failed to generate function mappings", ex)
      }
    }

    val constantsDomainBuilder = ConstantsDomainBuilder.from(_kbConstants)

    val generatedDomainMap = unionDomains(df = generatedDomains, aliases = _domainAliases)

    for ((name, symbolsRDD) <- domainMap)
      constantsDomainBuilder ++=(name, symbolsRDD.collect())

    for ((name, symbolsRDD) <- generatedDomainMap)
      constantsDomainBuilder ++=(name, symbolsRDD.collect())

    val constantsDomain = constantsDomainBuilder.result()

    // ---
    // --- Create a new evidence builder
    // ---
    // With evidence builder we can incrementally create an evidence database for LoMRF
    val trainingDB = EvidenceBuilder(_predicateSchema, _functionSchema, _queryPredicates, hiddenPredicates = Set.empty, constantsDomain)

    // ---
    // --- Store previously computed function mappings
    // ---
    for ((_, fm) <- functionMappings) {
      trainingDB.functions ++= fm.collect()
      fm.unpersist()
    }

    // ---
    // --- Create auxiliary predicates and give them as evidence facts:
    // ---
    // Compute instances of the auxiliary derived atoms from the raw data in the specified temporal interval.
    info(s"Generating derived events for the temporal interval [$startTime, $endTime]")

    for {transformation <- _ruleTransformations
         transformedRule = transformation.transformedRule
         (derivedAtom, sqlConstraint) <- transformation.atomMappings} {

      val terms = transformation.schema(derivedAtom.signature).mkString(",")
      val arity = derivedAtom.arity
      val symbol = derivedAtom.symbol

      val query = s"SELECT $terms FROM ${TrainingBatchLoader.tempEvidenceTableName} WHERE $sqlConstraint"

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

    // ---
    // --- Create ground-truth predicates (HoldsAt/2)
    // ---
    info(s"Generating annotation predicates for the temporal interval [$startTime, $endTime]")
    val headSignatures = _ruleTransformations.map(_.transformedRule.clause.head.signature).toSet

    val fluents = _kbDefiniteClauses
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
            for{
              mapper <- functionMappingsMap.value.get(fluentSignature)
              resultingSymbol <- mapper.get(sterms.map(_.toText))
              groundTerms = Vector(Constant(resultingSymbol), Constant(row.get(0).toString))
            } yield EvidenceAtom.asTrue("HoldsAt", groundTerms)

          } else None
        }

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

    }

    val trainingEvidence = trainingDB.result()

    locationDF.unpersist()
    annotationsDF.unpersist()
    annotatedLocations.unpersist()

    val definiteClauses = (_kbDefiniteClauses -- _ruleTransformations.map(_.originalRule)) ++ _ruleTransformations.map(_.transformedRule)

    val completedFormulas = PredicateCompletion(kb.formulas, definiteClauses)(_predicateSchema, kb.functionSchema, trainingEvidence.constants)

    def initialiseWeight(formula: WeightedFormula): WeightedFormula = {
      if (formula.weight.isNaN) formula.copy(weight = 1.0)
      else formula
    }

    val clauses = NormalForm
      .compileCNF(completedFormulas.map(initialiseWeight))(trainingEvidence.constants)
      .toVector

    // Give the resulting TrainingBatch for the specified interval
    TrainingBatch(
      mlnSchema = MLNSchema(_predicateSchema, kb.functionSchema, kb.dynamicPredicates, kb.dynamicFunctions),
      trainingEvidence,
      nonEvidenceAtoms = _queryPredicates,
      clauses)

  }
}

object TrainingBatchLoader {
  private val tempEvidenceTableName = "TEMP_EVIDENCE"
}
