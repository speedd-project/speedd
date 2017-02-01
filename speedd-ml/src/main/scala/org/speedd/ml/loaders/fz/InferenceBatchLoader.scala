package org.speedd.ml.loaders.fz

import lomrf.logic.{AtomSignature, NormalForm, Term, Variable, _}
import lomrf.mln.learning.structure.TrainingEvidence
import lomrf.mln.model._
import lomrf.util.Cartesian.CartesianIterator
import org.speedd.ml.loaders.{BatchLoader, InferenceBatch}
import org.speedd.ml.model.fz.InputData
import org.speedd.ml.util.data.DatabaseManager._
import slick.driver.PostgresDriver.api._
import org.speedd.ml.util.data._
import org.speedd.ml.util.logic._

final class InferenceBatchLoader(kb: KB,
                                 kbConstants: ConstantsDomain,
                                 predicateSchema: PredicateSchema,
                                 evidencePredicates: Set[AtomSignature],
                                 queryPredicates: Set[AtomSignature],
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
    * @param useOnlyConstants a subset of constant domain to be used (optional)
    *
    * @return a batch [[org.speedd.ml.loaders.Batch]] subclass specified during implementation
    */
  def forInterval(startTs: Int, endTs: Int, simulationId: Option[Int] = None,
                  useOnlyConstants: Option[DomainMap] = None): InferenceBatch = {

    val (constantsDomain, functionMappings, annotatedCards) =
      loadAll[Int, String, Int, Int](kbConstants, kb.functionSchema, None, startTs, endTs, simulationId, loadFor)

    // ---
    // --- Create a new evidence builder
    // ---
    // Evidence builder can incrementally create an evidence database. Everything not given is considered as false (CWA).
    val annotatedDB = EvidenceBuilder(predicateSchema, kb.functionSchema, queryPredicates, hiddenPredicates = Set.empty, constantsDomain)
      .withDynamicFunctions(predef.dynFunctions).withCWAForAll(true)

    // ---
    // --- Store previously computed function mappings
    // ---
    for ((_, fm) <- functionMappings)
      annotatedDB.functions ++= fm

    val functionMappingsMap = annotatedDB.functions.result()

    // ---
    // --- Create 'Next' predicates and give them as evidence facts:
    // ---
    info(s"Generating 'Next' predicates for the temporal interval [$startTs, $endTs]")
    /*(startTs to endTs).sliding(2).foreach {
      case pair => annotatedDB.evidence ++= EvidenceAtom.asTrue("Next", pair.map(t => Constant(t.toString)).reverse.toVector)
    }*/
    constantsDomain.get("card_pan").get.foreach { cardPan =>
      val timepoints = blockingExec {
        sql"""select timestamp from #${InputData.baseTableRow.schemaName.get}.#${InputData.baseTableRow.tableName}
              where card_pan = $cardPan AND timestamp between #$startTs AND #$endTs""".as[Int]
      }
      if (timepoints.length > 1) timepoints.sliding(2).foreach {
        case pair =>
          println(s"Next(${(Vector(Constant(cardPan)) ++ pair.map(t => Constant(t.toString)).reverse.toVector).mkString(", ")})")
          val nextAtom = EvidenceAtom.asTrue("Next", Vector(Constant(cardPan)) ++ pair.map(t => Constant(t.toString)).reverse.toVector)
          annotatedDB.evidence ++= nextAtom
      }
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

      val happensAtInstances = iterator.map(_.map(Constant))
        .flatMap { case constants =>

          /*val timeStamps = blockingExec {
            sql"""select timestamp from #${InputData.baseTableRow.schemaName.get}.#${InputData.baseTableRow.tableName}
                  where #${bindSQLVariables(sqlFunction.sqlConstraint, constants)}
                  AND timestamp between #$startTs AND #$endTs""".as[Int]
          }

          for {
            t <- timeStamps
            mapper <- functionMappingsMap.get(eventSignature)
            resultingSymbol <- mapper.get(constants.map(_.toText).toVector)
            groundTerms = Vector(Constant(resultingSymbol), Constant(t.toString))
          } yield EvidenceAtom.asTrue("HappensAt", groundTerms)*/

          val exist = blockingExec {
            sql"""select timestamp from #${InputData.baseTableRow.schemaName.get}.#${InputData.baseTableRow.tableName}
                  where #${bindSQLVariables(sqlFunction.sqlConstraint, constants)}""".as[Int]
          }.nonEmpty

          if (exist) {
            println(s"$symbol(${constants.toVector.mkString(",")})")
            Some(EvidenceAtom.asTrue(symbol, constants.toVector))
          }
          else None
        }

      for (happensAt <- happensAtInstances)
        annotatedDB.evidence += happensAt
    }

    // ---
    // --- Create ground-truth predicates (HoldsAt/2)
    // ---
    info(s"Generating annotation predicates for the temporal interval [$startTs, $endTs]")

    // Find all function signatures that have a fluent return type in the given KB
    /*val fluents = kb.formulas.flatMap(_.toCNF(kbConstants)).flatMap { c =>
      c.literals.filter(l => queryPredicates.contains(l.sentence.signature)).map(_.sentence.terms.head).flatMap {
        case TermFunction(symbol, terms, "fluent") => Some((symbol, terms, kb.functionSchema(AtomSignature(symbol, terms.length))._2))
        case _ => None
      }
    }*/

    //for ((symbol, terms, domain) <- fluents) {

      val holdsAtInstances = annotatedCards.flatMap { r =>

        val domainMap = Map[String, Constant](
          "timestamp" -> Constant(r._1.toString),
          "card_pan" -> Constant(r._2.toString),
          "card_exp_date" -> Constant(r._3.toString)
        )

        /*val constantsExist =
          for { t <- terms
                d <- domain
                if t.isConstant
          } yield domainMap(d) == t

        val theta = terms.withFilter(_.isVariable)
          .map(_.asInstanceOf[Variable])
          .map(v => v -> domainMap(v.domain))
          .toMap[Term, Term]

        val substitutedTerms = terms.map(_.substitute(theta))

        val fluentSignature = AtomSignature(symbol, substitutedTerms.size)*/

        if(r._4 == 1 /*&& constantsExist.forall(_ == true)*/) {
          println(s"Fraud(${Vector(Constant(r._2.toString), Constant(r._1.toString)).mkString(",")})")
          Some(EvidenceAtom.asTrue("Fraud", Vector(Constant(r._2.toString), Constant(r._1.toString))))
        }
          /*for {
            mapper <- functionMappingsMap.get(fluentSignature)
            resultingSymbol <- mapper.get(substitutedTerms.map(_.toText))
            groundTerms = Vector(Constant(resultingSymbol), Constant(r._1.toString))
          } yield EvidenceAtom.asTrue("HoldsAt", groundTerms)*/
        else None
      }


    /*val filteredFrauds =
      if(holdsAtInstances.nonEmpty) holdsAtInstances.sliding(2).flatMap { atomPairs =>
        if (atomPairs.last.terms.last.symbol.toInt - atomPairs.head.terms.last.symbol.toInt > 1 ||
          atomPairs.head.terms.head.symbol != atomPairs.last.terms.head.symbol)
          Some(atomPairs.head)
        else None
      } ++ Some(holdsAtInstances.last)
      else
        holdsAtInstances*/

      for (holdsAt <- holdsAtInstances) //try {
        annotatedDB.evidence += holdsAt
      /*} catch {
        case ex: java.util.NoSuchElementException =>
          val fluent = holdsAt.terms.head
          val timestamp = holdsAt.terms.last

          constantsDomain("fluent").get(fluent.symbol) match {
            case None => error(s"fluent constant ${fluent.symbol} is missing from constants domain}")
            case _ =>
          }

          constantsDomain("timestamp").get(timestamp.symbol) match {
            case None => error(s"timestamp constant ${timestamp.symbol} is missing from constants domain}")
            case _ =>
          }
      }*/
    //}

    // ---
    // --- Extract evidence and annotation
    // ---
    val (evidence, annotationDB) = extractAnnotation(annotatedDB.result(), evidencePredicates, queryPredicates)

    // ---
    // --- Compile clauses
    // ---
    val clauses = NormalForm
      .compileCNF(kb.formulas)(evidence.constants)
      .toVector

    InferenceBatch(
      mlnSchema = MLNSchema(predicateSchema, kb.functionSchema, kb.dynamicPredicates, kb.dynamicFunctions),
      evidence,
      annotationDB,
      queryPredicates,
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
    * @param excludeConstants a constant domain to be excluded (optional)
    * @return a training evidence batch [[lomrf.mln.learning.structure.TrainingEvidence]]
    */
  override def forIntervalSL(startTs: Int, endTs: Int, simulationId: Option[Int] = None,
                             excludeConstants: Option[DomainMap] = None): TrainingEvidence =
    fatal("Cannot load structure learning training batch during inference.")
}
