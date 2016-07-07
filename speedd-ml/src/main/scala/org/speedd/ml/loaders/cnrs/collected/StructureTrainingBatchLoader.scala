package org.speedd.ml.loaders.cnrs.collected

import auxlib.log.Logging
import lomrf.logic._
import lomrf.mln.learning.structure.TrainingEvidence
import lomrf.mln.model._
import lomrf.util.Cartesian.CartesianIterator
import org.speedd.ml.loaders.BatchLoader
import org.speedd.ml.util.logic._
import scala.util.{Failure, Success}
import slick.driver.PostgresDriver.api._
import org.speedd.ml.util.data.DatabaseManager._

class StructureTrainingBatchLoader(kb: KB,
                                   kbConstants: ConstantsDomain,
                                   predicateSchema: PredicateSchema,
                                   queryPredicates: Set[AtomSignature],
                                   evidencePredicates: Set[AtomSignature],
                                   sqlFunctionMappings: List[TermMapping]) extends Logging {

  def forInterval(startTs: Int, endTs: Int, simulationId: Option[Int] = None): TrainingEvidence = {

    val (domainsMap, annotatedLocations) = loadFor(simulationId.get, startTs, endTs)

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
    val trainingDBWithFunctions = EvidenceBuilder(predicateSchema, kb.functionSchema, queryPredicates, hiddenPredicates = Set.empty, constantsDomain)
      .withDynamicFunctions(predef.dynFunctions).withCWAForAll(true)

    val trainingDBWithoutFunctions = EvidenceBuilder(predicateSchema, kb.functionSchema, queryPredicates, hiddenPredicates = Set.empty,
      constantsDomain, convertFunctionsToPredicates = true).withDynamicFunctions(predef.dynFunctions).withCWAForAll(true)

    (startTs to endTs).sliding(2).foreach { pair =>
      val atom = EvidenceAtom.asTrue("Next", pair.map(t => Constant(t.toString)).reverse.toVector)
      trainingDBWithFunctions.evidence ++= atom
      trainingDBWithoutFunctions.evidence ++= atom
    }

    // ---
    // --- Store previously computed function mappings
    // ---
    for ((_, fm) <- functionMappings) {
      trainingDBWithFunctions.functions ++= fm
      trainingDBWithoutFunctions.functions ++= fm
    }

    debug(s"Domains:\n${constantsDomain.map(a => s"${a._1} -> [${a._2.mkString(", ")}]").mkString("\n")}")

    val functionMappingsMap = trainingDBWithFunctions.functions.result()

    // ---
    // --- Create auxiliary predicates and give them as evidence facts:
    // ---
    // Compute instances of the auxiliary derived atoms from the raw data in the specified temporal interval.
    info(s"Generating derived events for the temporal interval [$startTs, $endTs]")

    for (sqlFunction <- sqlFunctionMappings) {

      val domain = sqlFunction.domain
      val arity = sqlFunction.domain.length
      val symbol = sqlFunction.symbol
      val eventSignature = AtomSignature(symbol, arity)

      val argDomainValues = domain.map(t => domainsMap.get(t).get)

      val iterator = CartesianIterator(argDomainValues)

      iterator.map(_.map(Constant))
        .foreach { case constants =>

          val time_points = blockingExec {
            sql"""select timestamp from cnrs.input where #${bindSQLVariables(sqlFunction.sqlConstraint, constants)}
                  AND timestamp between #$startTs AND #$endTs""".as[Int]
          }

          val happens = for {
            t <- time_points
            mapper <- functionMappingsMap.get(eventSignature)
            resultingSymbol <- mapper.get(constants.map(_.toText).toVector)
            groundTerms = Vector(Constant(resultingSymbol), Constant(t.toString))
          } yield EvidenceAtom.asTrue("HappensAt", groundTerms)

          for (h <- happens) {
            trainingDBWithFunctions.evidence += h
            trainingDBWithoutFunctions.evidence += h
          }
        }
    }

    // ---
    // --- Create ground-truth predicates (HoldsAt/2)
    // ---
    info(s"Generating annotation predicates for the temporal interval [$startTs, $endTs]")

    val fluents = kb.functionSchema.filter(f => f._2._1 == "fluent")

    for ((fluentSignature, (ret, domain)) <- fluents) {

      val holdsAtInstances = annotatedLocations.flatMap { r =>

        val domainMap = Map[String, Constant](
          "timestamp" -> Constant(r._1.toString),
          "loc_id" -> Constant(r._2.toString),
          "lane" -> Constant(r._3.toString)
        )

        val tuple = domain.map(domainMap)

        if(r._4.isDefined && r._4.get == fluentSignature.symbol)
          for {
            mapper <- functionMappingsMap.get(fluentSignature)
            resultingSymbol <- mapper.get(tuple.map(_.toText))
            groundTerms = Vector(Constant(resultingSymbol), Constant(r._1.toString))
          } yield EvidenceAtom.asTrue("HoldsAt", groundTerms)
        else None
      }

      for (atom <- holdsAtInstances) try {
        trainingDBWithFunctions.evidence += atom
        trainingDBWithoutFunctions.evidence += atom
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

    val trainingEvidenceWithFunctions = trainingDBWithFunctions.result()
    val trainingEvidenceWithoutFunctions = trainingDBWithoutFunctions.result()

    @inline
    def extractAnnotation(trainingEvidence: Evidence, evidenceAtoms: Set[AtomSignature]): (Evidence, EvidenceDB) = {

      // Partition the training data into annotation and evidence databases
      var (annotationDB, atomStateDB) = trainingEvidence.db.partition(e => queryPredicates.contains(e._1))

      // Define all non evidence atoms as unknown in the evidence database
      for (signature <- annotationDB.keysIterator)
        atomStateDB += (signature -> AtomEvidenceDB.allUnknown(trainingEvidence.db(signature).identity))

      // Define all non evidence atoms for which annotation was not given as false in the annotation database (close world assumption)
      for (signature <- queryPredicates; if !annotationDB.contains(signature)) {
        //warn(s"Annotation was not given in the training file(s) for predicate '$signature', assuming FALSE state for all its groundings.")
        annotationDB += (signature -> AtomEvidenceDB.allFalse(trainingEvidence.db(signature).identity))
      }

      // Define all not seen evidence atoms but existing in the predicate schema as false due to close world assumption
      for (signature <- kb.predicateSchema.keysIterator; if !atomStateDB.contains(signature)) {
        if (evidenceAtoms.contains(signature))
          atomStateDB += (signature -> AtomEvidenceDB.allFalse(trainingEvidence.db(signature).identity))
      }

      (new Evidence(trainingEvidence.constants, atomStateDB, trainingEvidence.functionMappers), annotationDB)
    }

    val (evidence, annotation) = extractAnnotation(trainingEvidenceWithFunctions, evidencePredicates)

    val (convertedEvidence, _) = extractAnnotation(trainingEvidenceWithoutFunctions, evidencePredicates)

    TrainingEvidence(evidence, annotation, convertedEvidence)
  }
}
