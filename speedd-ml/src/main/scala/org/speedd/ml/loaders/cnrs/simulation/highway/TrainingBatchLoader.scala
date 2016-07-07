package org.speedd.ml.loaders.cnrs.simulation.highway

import lomrf.logic.{NormalForm, WeightedFormula, _}
import lomrf.mln.learning.structure.TrainingEvidence
import org.speedd.ml.loaders.{BatchLoader, TrainingBatch}
import org.speedd.ml.util.logic._
import lomrf.mln.model._
import lomrf.util.Cartesian.CartesianIterator
import org.speedd.ml.model.cnrs.simulation.highway.{InputData, LocationData}
import org.speedd.ml.util.data.DatabaseManager._
import slick.driver.PostgresDriver.api._

final class TrainingBatchLoader(kb: KB,
                                kbConstants: ConstantsDomain,
                                predicateSchema: PredicateSchema,
                                evidencePredicates: Set[AtomSignature],
                                nonEvidencePredicates: Set[AtomSignature],
                                sqlFunctionMappings: List[TermMapping]) extends BatchLoader {

  /**
    * Loads a (micro) batch, either training [[org.speedd.ml.loaders.TrainingBatch]] or
    * inference [[org.speedd.ml.loaders.InferenceBatch]], that holds the data for the
    * specified interval. Furthermore, a simulation id can be provided in case of
    * simulation data.
    *
    * @param startTs starting time point
    * @param endTs end time point
    * @param simulationId simulation id (optional)
    *
    * @return a batch [[org.speedd.ml.loaders.Batch]] subclass specified during implementation
    */
  override def forInterval(startTs: Int, endTs: Int, simulationId: Option[Int] = None): TrainingBatch = {

    val (constantsDomain, functionMappings, annotatedLocations) =
      loadAll[Int, Int, Int, String](kbConstants, kb.functionSchema, startTs, endTs, simulationId, loadFor)

    // ---
    // --- Create a new evidence builder
    // ---
    // Evidence builder can incrementally create an evidence database. Everything not given is considered as false (CWA).
    val trainingDB = EvidenceBuilder(predicateSchema, kb.functionSchema, nonEvidencePredicates, hiddenPredicates = Set.empty, constantsDomain)
      .withDynamicFunctions(predef.dynFunctions).withCWAForAll(true)

    // ---
    // --- Store previously computed function mappings
    // ---
    for ((_, fm) <- functionMappings)
      trainingDB.functions ++= fm

    val functionMappingsMap = trainingDB.functions.result()

    // ---
    // --- Create 'Next' predicates and give them as evidence facts:
    // ---
    info(s"Generating 'Next' predicates for the temporal interval [$startTs, $endTs]")
    (startTs to endTs).sliding(2).foreach {
      case pair => trainingDB.evidence ++= EvidenceAtom.asTrue("Next", pair.map(t => Constant(t.toString)).reverse.toVector)
    }

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

      val argDomainValues = domain.map(t => constantsDomain.get(t).get).map(_.toIterable)

      val iterator = CartesianIterator(argDomainValues)

      iterator.map(_.map(Constant))
        .foreach { case constants =>

          val detectorId = blockingExec {
            LocationData.filter(l => l.sectionId === constants.head.symbol.toInt)
              .map(_.detectorId).distinct.result
          }.head

          val time_points = blockingExec {
            sql"""select timestamp from #${InputData.baseTableRow.schemaName.get}.#${InputData.baseTableRow.tableName}
                  where #${bindSQLVariables(sqlFunction.sqlConstraint, Array(Constant(detectorId.toString)))}
                  AND simulation_id = #${simulationId.get} AND timestamp between #$startTs AND #$endTs""".as[Int]
          }

          val happens = for {
            t <- time_points
            mapper <- functionMappingsMap.get(eventSignature)
            resultingSymbol <- mapper.get(constants.map(_.toText).toVector)
            groundTerms = Vector(Constant(resultingSymbol), Constant(t.toString))
          } yield EvidenceAtom.asTrue("HappensAt", groundTerms)

          for (h <- happens)
            trainingDB.evidence += h
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
          "section_id" -> Constant(r._3.toString)
        )

        val tuple = domain.map(domainMap)

        if(r._4 == fluentSignature.symbol)
          for {
            mapper <- functionMappingsMap.get(fluentSignature)
            resultingSymbol <- mapper.get(tuple.map(_.toText))
            groundTerms = Vector(Constant(resultingSymbol), Constant(r._1.toString))
          } yield EvidenceAtom.asTrue("HoldsAt", groundTerms)
        else None
      }

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

    val completedFormulas =
      PredicateCompletion(kb.formulas, kb.definiteClauses)(predicateSchema, kb.functionSchema, trainingEvidence.constants)

    def initialiseWeight(formula: WeightedFormula): WeightedFormula = {
      if (formula.weight.isNaN) formula.copy(weight = 1.0)
      else formula
    }

    val clauses = NormalForm
      .compileCNF(completedFormulas.map(initialiseWeight))(trainingEvidence.constants)
      .toVector

    val (evidence, annotationDB) = extractAnnotation(trainingEvidence, evidencePredicates, nonEvidencePredicates)

    // Return the resulting TrainingBatch for the specified interval
    TrainingBatch(
      mlnSchema = MLNSchema(predicateSchema, kb.functionSchema, kb.dynamicPredicates, kb.dynamicFunctions),
      evidence,
      annotationDB,
      nonEvidenceAtoms = nonEvidencePredicates,
      clauses)
  }

  /**
    * Loads a training evidence batch [[lomrf.mln.learning.structure.TrainingEvidence]] that holds
    * the data for the specified interval. Furthermore, a simulation id can be provided in case of
    * simulation data. Should be used only for loading data during structure learning.
    *
    * @param startTs starting time point
    * @param endTs end time point
    * @param simulationId simulation id (optional)
    *
    * @return a training evidence batch [[lomrf.mln.learning.structure.TrainingEvidence]]
    */
  override def forIntervalSL(startTs: Int, endTs: Int, simulationId: Option[Int] = None): TrainingEvidence = {

    val (constantsDomain, functionMappings, annotatedLocations) =
      loadAll[Int, Int, Int, String](kbConstants, kb.functionSchema, startTs, endTs, simulationId, loadFor)

    // ---
    // --- Create a new evidence builder
    // ---
    // Evidence builder can incrementally create an evidence database. Everything not given is considered as false (CWA).
    val trainingDB = EvidenceBuilder(predicateSchema, kb.functionSchema, nonEvidencePredicates, hiddenPredicates = Set.empty, constantsDomain)
      .withDynamicFunctions(predef.dynFunctions).withCWAForAll(true)

    val trainingDBNoFunctions = EvidenceBuilder(predicateSchema, kb.functionSchema, nonEvidencePredicates, hiddenPredicates = Set.empty,
      constantsDomain, convertFunctionsToPredicates = true).withDynamicFunctions(predef.dynFunctions).withCWAForAll(true)

    // ---
    // --- Store previously computed function mappings
    // ---
    for ((_, fm) <- functionMappings) {
      trainingDB.functions ++= fm
      trainingDBNoFunctions.functions ++= fm
    }

    val functionMappingsMap = trainingDB.functions.result()

    // ---
    // --- Create 'Next' predicates and give them as evidence facts:
    // ---
    info(s"Generating 'Next' predicates for the temporal interval [$startTs, $endTs]")
    (startTs to endTs).sliding(2).foreach { pair =>
      val nextAtom = EvidenceAtom.asTrue("Next", pair.map(t => Constant(t.toString)).reverse.toVector)
      trainingDB.evidence ++= nextAtom
      trainingDBNoFunctions.evidence ++= nextAtom
    }

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

      val argDomainValues = domain.map(t => constantsDomain.get(t).get).map(_.toIterable)

      val iterator = CartesianIterator(argDomainValues)

      iterator.map(_.map(Constant))
        .foreach { case constants =>

          val detectorId = blockingExec {
            LocationData.filter(l => l.sectionId === constants.head.symbol.toInt)
              .map(_.detectorId).distinct.result
          }.head

          val time_points = blockingExec {
            sql"""select timestamp from #${InputData.baseTableRow.schemaName.get}.#${InputData.baseTableRow.tableName}
                  where #${bindSQLVariables(sqlFunction.sqlConstraint, Array(Constant(detectorId.toString)))}
                  AND simulation_id = #${simulationId.get} AND timestamp between #$startTs AND #$endTs""".as[Int]
          }

          val happens = for {
            t <- time_points
            mapper <- functionMappingsMap.get(eventSignature)
            resultingSymbol <- mapper.get(constants.map(_.toText).toVector)
            groundTerms = Vector(Constant(resultingSymbol), Constant(t.toString))
          } yield EvidenceAtom.asTrue("HappensAt", groundTerms)

          for (h <- happens) {
            trainingDB.evidence += h
            trainingDBNoFunctions.evidence += h
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
          "section_id" -> Constant(r._3.toString)
        )

        val tuple = domain.map(domainMap)

        if(r._4 == fluentSignature.symbol)
          for {
            mapper <- functionMappingsMap.get(fluentSignature)
            resultingSymbol <- mapper.get(tuple.map(_.toText))
            groundTerms = Vector(Constant(resultingSymbol), Constant(r._1.toString))
          } yield EvidenceAtom.asTrue("HoldsAt", groundTerms)
        else None
      }

      for (atom <- holdsAtInstances) try {
        trainingDB.evidence += atom
        trainingDBNoFunctions.evidence += atom
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

    val (evidence, annotation) =
      extractAnnotation(trainingDB.result(), evidencePredicates, nonEvidencePredicates)

    val (convertedEvidence, _) =
      extractAnnotation(trainingDBNoFunctions.result(), evidencePredicates, nonEvidencePredicates)

    TrainingEvidence(evidence, annotation, convertedEvidence)
  }

}
