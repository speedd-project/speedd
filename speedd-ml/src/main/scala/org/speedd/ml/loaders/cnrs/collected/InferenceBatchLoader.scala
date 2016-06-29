package org.speedd.ml.loaders.cnrs.collected

import lomrf.logic._
import lomrf.mln.model._
import lomrf.util.Cartesian.CartesianIterator
import org.speedd.ml.loaders.{BatchLoader, InferenceBatch}
import org.speedd.ml.util.data.DatabaseManager._
import slick.driver.PostgresDriver.api._
import org.speedd.ml.util.logic._

import scala.collection.breakOut
import scala.util.{Failure, Success}

final class InferenceBatchLoader(kb: KB,
                                 kbConstants: ConstantsDomain,
                                 predicateSchema: PredicateSchema,
                                 queryPredicates: Set[AtomSignature],
                                 /*atomMappings: List[TermMapping],*/
                                 sqlFunctionMappings: List[TermMapping]) extends BatchLoader {

  def forInterval(startTs: Int, endTs: Int): InferenceBatch = {

    val (domainsMap, annotatedLocations) = loadFor(startTs, endTs)

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
    val annotatedDB = EvidenceBuilder(predicateSchema, kb.functionSchema, queryPredicates, hiddenPredicates = Set.empty, constantsDomain)
      .withDynamicFunctions(predef.dynFunctions).withCWAForAll(true)

    // ---
    // --- Store previously computed function mappings
    // ---
    for ((_, fm) <- functionMappings)
      annotatedDB.functions ++= fm

    (startTs to endTs).sliding(2).foreach { pair =>
      val atom = EvidenceAtom.asTrue("Next", pair.map(t => Constant(t.toString)).reverse.toVector)
      annotatedDB.evidence ++= atom
    }

    debug(s"Domains:\n${constantsDomain.map(a => s"${a._1} -> [${a._2.mkString(", ")}]").mkString("\n")}")

    /*for (atomMapping <- atomMappings) {

      val terms = atomMapping.domain.mkString(",")
      val arity = atomMapping.domain.length
      val symbol = atomMapping.symbol

      val symbols =
        """[O|S][0-9]""".r.findAllMatchIn {
          atomMapping.sqlConstraint
        }.map(_ group 0).toIndexedSeq

      val intervals = symbols.map(symbol => symbols2udf(symbol.head.toString)(symbol))

      debug(s"${symbols.mkString(", ")} -> ${intervals.map(_.toString).mkString(", ")}")

      var result = atomMapping.sqlConstraint
      for(i <- symbols.indices)
        result = result.replace(s"${symbols2domain(symbols(i).head.toString)} = '${symbols(i)}'", intervals(i))

      // TODO As Int should be changed if the derived atoms have different domain
      annotatedDB.evidence ++= blockingExec {
        sql"""select distinct #$terms from cnrs.input where timestamp >= #$startTs and timestamp <= #$endTs and #$result""".as[Int]
      }.map { t =>
        val constants: Vector[Constant] = (0 until arity).map(i => Constant(t.toString))(breakOut)
        EvidenceAtom.asTrue(symbol, constants)
      }
    }*/

    val functionMappingsMap = annotatedDB.functions.result()

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

          for (h <- happens)
            annotatedDB.evidence += h
        }
    }

    // ---
    // --- Create ground-truth predicates (HoldsAt/2)
    // ---
    info(s"Generating annotation predicates for the temporal interval [$startTs, $endTs]")

    val fluents = kb.formulas.flatMap(_.toCNF(kbConstants)).flatMap { c =>
      c.literals.filter(l => queryPredicates.contains(l.sentence.signature)).map(_.sentence.terms.head).flatMap {
        case TermFunction(symbol, terms, "fluent") => Some((symbol, terms, kb.functionSchema(AtomSignature(symbol, terms.length))._2))
        case _ => None
      }
    }

    for ((symbol, terms, domain) <- fluents) {

      /*val holdsAtInstances = annotatedLocations.flatMap { r =>

        val domainMap = Map[String, Constant](
          "timestamp" -> Constant(r._1.toString),
          "loc_id" -> Constant(r._2.toString)
        )

        /* Check if the constants of the fluent exist in the current tuple. If not then
         * the tuple will be discarded in the following code.
         */
        val constantsExist =
          for { t <- terms
                d <- domain
                if t.isConstant
          } yield domainMap(d) == t

        val theta = terms.withFilter(_.isVariable)
          .map(_.asInstanceOf[Variable])
          .map(v => v -> domainMap(v.domain))
          .toMap[Term, Term]

        val substitutedTerms = terms.map(_.substitute(theta))

        if (substitutedTerms.forall(_.isConstant) && constantsExist.forall(_ == true)) {
          val fluentSignature = AtomSignature(symbol, substitutedTerms.size)
          if(r._4.isDefined && r._4.get == symbol)
            for {
              mapper <- functionMappingsMap.get(fluentSignature)
              resultingSymbol <- mapper.get(substitutedTerms.map(_.toText))
              groundTerms = Vector(Constant(resultingSymbol), Constant(r._1.toString))
            } yield (EvidenceAtom.asTrue("HoldsAt", groundTerms), substitutedTerms)
          else
            for {
              mapper <- functionMappingsMap.get(fluentSignature)
              resultingSymbol <- mapper.get(substitutedTerms.map(_.toText))
              groundTerms = Vector(Constant(resultingSymbol), Constant(r._1.toString))
            } yield (EvidenceAtom.asFalse("HoldsAt", groundTerms), substitutedTerms)
        } else None
      }*/

      val holdsAtInstances = annotatedLocations.flatMap { r =>

        val domainMap = Map[String, Constant](
          "timestamp" -> Constant(r._1.toString),
          "loc_id" -> Constant(r._2.toString),
          "lane" -> Constant(r._3.toString)
        )

        val tuple = domain.map(domainMap)

        val fluentSignature = AtomSignature(symbol, terms.size)

        if(r._4.isDefined && r._4.get == fluentSignature.symbol)
          for {
            mapper <- functionMappingsMap.get(fluentSignature)
            resultingSymbol <- mapper.get(tuple.map(_.toText))
            groundTerms = Vector(Constant(resultingSymbol), Constant(r._1.toString))
          } yield EvidenceAtom.asTrue("HoldsAt", groundTerms)
        else None
      }

      for (atom <- holdsAtInstances) try {
        annotatedDB.evidence += atom
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

    val annotatedEvidence = annotatedDB.result()

    info(annotatedEvidence.db.values.mkString("\n"))

    val domainSpace = PredicateSpace(kb.schema, queryPredicates, annotatedEvidence.constants)

    var (annotationDB, atomStateDB) = annotatedEvidence.db.partition(e => queryPredicates.contains(e._1))

    // Define all non evidence atoms as unknown in the evidence database
    for (signature <- annotationDB.keysIterator)
      atomStateDB += (signature -> AtomEvidenceDB.allUnknown(domainSpace.identities(signature)))

    // Define all non evidence atoms for which annotation was not given as false in the annotation database (close world assumption)
    for (signature <- queryPredicates; if !annotationDB.contains(signature)) {
      warn(s"Annotation was not given in the training file(s) for predicate '$signature', assuming FALSE state for all its groundings.")
      annotationDB += (signature -> AtomEvidenceDB.allFalse(domainSpace.identities(signature)))
    }

    for (signature <- kb.predicateSchema.keysIterator; if !atomStateDB.contains(signature)) {
      if ((kb.predicateSchema.keySet -- queryPredicates).contains(signature))
        atomStateDB += (signature -> AtomEvidenceDB.allFalse(domainSpace.identities(signature)))
    }

    val evidence = new Evidence(annotatedEvidence.constants, atomStateDB, annotatedEvidence.functionMappers)

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
}
