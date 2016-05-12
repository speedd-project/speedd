package org.speedd.ml.loaders.cnrs.collected

import lomrf.logic._
import lomrf.mln.model._
import org.speedd.ml.loaders.{BatchLoader, TrainingBatch}
import org.speedd.ml.model.cnrs.collected.{annotation, input, location}
import org.speedd.ml.util.logic._
import slick.driver.PostgresDriver.api._
import org.speedd.ml.util.data.DatabaseManager._

import scala.util.{Failure, Success}
import scala.collection.breakOut

final class TrainingBatchLoader(kb: KB,
                                kbConstants: ConstantsDomain,
                                predicateSchema: PredicateSchema,
                                queryPredicates: Set[AtomSignature],
                                ruleTransformations: Iterable[RuleTransformation]) extends BatchLoader {

  def loadFor(startTs: Int, endTs: Int,
              initial: ConstantsDomain = Map.empty) = {

    var domainsMap = initial.map(pair => pair._1 -> pair._2.toIterable)

    domainsMap += "timestamp" -> (startTs to endTs).map(_.toString)

    val inputValues =
      blockingExec {
        input.filter(i => i.timeStamp >= startTs && i.timeStamp <= endTs)
          .map(i => (i.occupancy, i.vehicles, i.avgSpeed)).result
      }.unzip3

    domainsMap ++= Iterable("occupancy" -> inputValues._1.flatten.map(domain2udf("occupancy")(_)).distinct,
                            "vehicles" -> inputValues._2.flatten.map(domain2udf("vehicles")(_)).distinct,
                            "avg_speed" -> inputValues._3.flatten.map(domain2udf("avg_speed")(_)).distinct)

    val locationValues = blockingExec {
      location.map(l => (l.locId, l.lane)).result
    }.map{ case (locId, lane) =>
      (locId.toString, lane)
    }.unzip

    domainsMap ++= Iterable("loc_id" -> locationValues._1.distinct,
                            "lane" -> locationValues._2.distinct)

    val annotationIntervalQuery =
      annotation.filter(a => a.endTs >= startTs && a.startTs <= endTs)

    domainsMap += "description" -> blockingExec {
      annotationIntervalQuery.map(_.description).result
    }.distinct

    val annotationTuples = blockingExec {
      location.join(annotationIntervalQuery)
        .on((a, b) => a.distance <= b.startLoc && a.distance >= b.endLoc)
        .map { case (loc, ann) =>
          (ann.startTs, ann.endTs, loc.locId, ann.description, loc.lane)
        }.result
    }.flatMap { case (startT, endT, locId, description, lane) =>
      (startT to endT).filter(ts => ts >= startTs && ts <= endTs)
        .map(ts => (ts, locId, description, lane))
    }

    (domainsMap, annotationTuples)
  }

  def forInterval(startTs: Int, endTs: Int): Unit /*TrainingBatch*/ = {

    val (domainsMap, annotation) = loadFor(startTs, endTs)

    val (functionMappings, generatedDomainMap) = {
      generateFunctionMappings(kb.functionSchema, domainsMap) match {
        case Success(result) => result
        case Failure(exception) => fatal("Failed to generate function mappings", exception)
      }
    }

    val constantsDomainBuilder = ConstantsDomainBuilder.from(kbConstants)

    for ((name, symbols) <- domainsMap)
      constantsDomainBuilder ++=(name, symbols)

    for ((name, symbols) <- generatedDomainMap)
      constantsDomainBuilder ++=(name, symbols)

    val constantsDomain = constantsDomainBuilder.result()

    // ---
    // --- Create a new evidence builder
    // ---
    // Evidence builder can incrementally create an evidence database for LoMRF
    val trainingDB = EvidenceBuilder(predicateSchema, kb.functionSchema, queryPredicates, hiddenPredicates = Set.empty, constantsDomain)
                      .withDynamicFunctions(predef.dynFunctions)

    // ---
    // --- Store previously computed function mappings
    // ---
    for ((_, fm) <- functionMappings)
      trainingDB.functions ++= fm

    debug(s"Domains:\n${constantsDomain.map(a => s"${a._1} -> [${a._2.mkString(", ")}]").mkString("\n")}")

    // ---
    // --- Create auxiliary predicates and give them as evidence facts:
    // ---
    // Compute instances of the auxiliary derived atoms from the raw data in the specified temporal interval.
    info(s"Generating derived events for the temporal interval [$startTs, $endTs]")

    for {transformation <- ruleTransformations
         transformedRule = transformation.transformedRule
         (derivedAtom, sqlConstraint) <- transformation.atomMappings} {

      val terms = transformation.schema(derivedAtom.signature).mkString(",")
      val arity = derivedAtom.arity
      val symbol = derivedAtom.symbol

      val symbols = """[O|S][0-9]""".r.findAllMatchIn {
        sqlConstraint
      }.map(_ group 0).toIndexedSeq

      val intervals = symbols.map(symbol => symbols2udf(symbol.head.toString)(symbol))

      debug(s"${symbols.mkString(", ")} -> ${intervals.map(_.toString).mkString(", ")}")

      var result = sqlConstraint
      for(i <- symbols.indices)
        result = result.replace(s"${symbols2domain(symbols(i).head.toString)} = '${symbols(i)}'", intervals(i))

      trainingDB.evidence ++= blockingExec {
        sql"""select distinct #$terms from cnrs.input where timestamp >= #$startTs and timestamp <= #$endTs and #$result""".as[Int]
      }.map { t =>
        val constants: Vector[Constant] = (0 until arity).map(i => Constant(t.toString))(breakOut)
        EvidenceAtom.asTrue(symbol, constants)
      }
    }

    val functionMappingsMap = trainingDB.functions.result()

    // ---
    // --- Create ground-truth predicates (HoldsAt/2)
    // ---
    info(s"Generating annotation predicates for the temporal interval [$startTs, $endTs]")
    val headSignatures = ruleTransformations.map(_.transformedRule.clause.head.signature).toSet

    val fluents = kb.definiteClauses
      .withFilter(wc => headSignatures.contains(wc.clause.head.signature))
      .map(_.clause.head.terms.head)
      .flatMap {
        case TermFunction(symbol, terms, "fluent") => Some((symbol, terms))
        case _ => None
      }



  }
}
