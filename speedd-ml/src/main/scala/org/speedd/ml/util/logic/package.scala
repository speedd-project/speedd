package org.speedd.ml.util

import java.io.{BufferedWriter, FileWriter, PrintWriter}
import lomrf.logic.{Constant, _}
import lomrf.mln.model._
import lomrf.util.Cartesian.CartesianIterator
import scala.collection.mutable
import scala.util.{Failure, Success, Try}
import lomrf.FUNC_RET_VAR_PREFIX
import org.speedd.ml.util.data.DomainMap

package object logic {

  /**
    * Default predefined collection of atomic signatures that correspond to predicates having
    * infix notation (e.g., =, >=, etc)
    */
  val INFIX_SIGNATURES = Set[AtomSignature](
    AtomSignature("equals", 2), // corresponding infix notation: =
    AtomSignature("not_equals", 2), // corresponding infix notation: !=
    AtomSignature("greaterThan", 2), // corresponding infix notation: >
    AtomSignature("greaterThanEq", 2), // corresponding infix notation: >=
    AtomSignature("lessThan", 2), // corresponding infix notation: <
    AtomSignature("lessThanEq", 2) // corresponding infix notation: <=
  )

  implicit class WrappedKB(val kb: KB) extends AnyVal {

    def simplify(inputSignatures: Set[AtomSignature], targetSignatures: Set[AtomSignature], domains: ConstantsDomain,
                 infixSignatures: Set[AtomSignature] = INFIX_SIGNATURES): Try[(Iterable[RuleTransformation], PredicateSchema)] = {

      KBSimplifier.simplify(kb, inputSignatures, targetSignatures, domains, infixSignatures)
    }
  }

  implicit class WrappedBody(val body: DefiniteClauseConstruct) extends AnyVal {

    def findAtoms(f: (AtomicFormula) => Boolean): Vector[AtomicFormula] = {
      body match {
        case a: AtomicFormula => if (f(a)) Vector(a) else Vector.empty[AtomicFormula]

        case _ =>
          val queue = mutable.Queue[Formula]()
          body.subFormulas.foreach(queue.enqueue(_))

          var resultingMatches = Vector[AtomicFormula]()

          while (queue.nonEmpty) {
            val currentConstruct = queue.dequeue()
            currentConstruct match {
              case a: AtomicFormula => if (f(a)) resultingMatches :+= a
              case _ => currentConstruct.subFormulas.foreach(f => queue.enqueue(f))
            }
          }

          resultingMatches
      }
    }
  }

  /**
    * Generates function mappings given the extracted domains and appends
    * the function return constants to the domain.
    *
    * @param functionSchema a function schema
    * @param domainsMap a set of domain mappings
    * @return an array of function mappings for each function signature and the refined domain mappings
    */
  def generateFunctionMappings(functionSchema: FunctionSchema, domainsMap: Map[String, Iterable[String]]):
                               Try[(Map[AtomSignature, Iterable[FunctionMapping]], DomainMap)] = {

    // ---
    // --- Compute function mappings
    // ---
    var functionMappings = Map.empty[AtomSignature, Iterable[FunctionMapping]]

    var generatedDomains = Map.empty[String, Iterable[String]]

    for(((signature, (retDomain, argDomains)), index) <- functionSchema.zipWithIndex) {
      val symbol = signature.symbol

      val argDomainValues = argDomains.map { name =>
        domainsMap.getOrElse(name, return Failure(new NoSuchElementException(s"Unknown domain name '$name'")))
      }

      val iterator = CartesianIterator(argDomainValues)

      val products = iterator.map { _.map(Constant) }.zipWithIndex.map {
        case (constants, uid) =>
          val retConstant = s"${FUNC_RET_VAR_PREFIX}_${index}_$uid"
          retConstant -> FunctionMapping(retConstant, symbol, constants.toVector)
      }.toMap

      // If constants exist for this return type domain, then append the the keys
      if (generatedDomains.contains(retDomain))
        generatedDomains += retDomain -> (generatedDomains(retDomain) ++ products.keys)
      else generatedDomains += retDomain -> products.keys

      functionMappings += signature -> products.values
    }

    Success((functionMappings, generatedDomains))
  }

  /**
    * Partition training evidence into evidence and annotation.
    *
    * @param trainingEvidence the training evidence, containing truth values for non evidence predicates
    * @param evidenceAtoms the set of evidence atom signatures
    * @param nonEvidenceAtoms the set of non evidence atom signatures
    *
    * @return a tuple containing evidence and annotation databases
    */
  def extractAnnotation(trainingEvidence: Evidence,
                        evidenceAtoms: Set[AtomSignature],
                        nonEvidenceAtoms: Set[AtomSignature]): (Evidence, EvidenceDB) = {

    // Partition the training data into annotation and evidence databases
    var (annotationDB, atomStateDB) = trainingEvidence.db.partition {
      case (signature, db) => nonEvidenceAtoms.contains(signature)
    }

    // Define all non evidence atoms as unknown in the evidence database
    for (signature <- annotationDB.keysIterator)
      atomStateDB += (signature -> AtomEvidenceDB.allUnknown(trainingEvidence.db(signature).identity))

    (new Evidence(trainingEvidence.constants, atomStateDB, trainingEvidence.functionMappers), annotationDB)
  }

  /**
    * Export all given rule transformations into the given output path.
    *
    * @param ruleTransformations an iterable of rule transformation instances
    * @param outputPath the output path to export the transformations
    * @param interval
    *
    * @return the output path
    */
  def exportTransformations(ruleTransformations: Iterable[RuleTransformation],
                            outputPath: String, interval: Option[(Long, Long)] = None) = Try[String] {

    // Overwrite file if any exists
    val p = new PrintWriter(new BufferedWriter(new FileWriter(outputPath, false)))

    interval match {
      case Some((startTime, endTime)) =>
        p.println(s"// Interval: [$startTime, $endTime]")
      case None =>
        p.println(s"// Interval: 'undefined'")
    }

    for(transformation <- ruleTransformations; (da, constraint) <- transformation.atomMappings) {
      val renamedDA = da.copy(terms = da.terms.map(v => v.copy(symbol = v.domain)))
      p.println(s"${renamedDA.toText} -> $constraint")
    }
    p.println()
    p.close()

    outputPath
  }

  /**
    * Parses the given string, which is expected to contain atomic signatures that are separated by commas. For example,
    * consider the following string:
    *
    * {{{
    *   parseSignatures(src = "HoldsAt/2,InitiatedAt/2, TerminatedAt/2")
    * }}}
    *
    * For the above string this function will produce the following successful result:
    *
    * {{{
    *   Success( Set( AtomSignature("HoldsAt", 2), AtomSignature("InitiatedAt", 2), AtomSignature("TerminatedAt", 2) ) )
    * }}}
    *
    * In situations where the given string is not following the expected format, this function will give a Failure with
    * the caused exception.
    *
    * @param src source string composed of comma separated atomic signatures
    * @return a Success try containing a collection of AtomSignatures from the specified source string, otherwise a
    *         Failure containing the caused exception.
    */
  def parseSignatures(src: String): Try[Set[AtomSignature]] = Try {
    src.split(",").map { entry =>
      val (symbol, arity) = entry.span(_ == '/')
      AtomSignature(symbol.trim, arity.trim.toInt)
    }(scala.collection.breakOut)
  }

}
