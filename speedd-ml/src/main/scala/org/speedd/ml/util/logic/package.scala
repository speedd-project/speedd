package org.speedd.ml.util

import java.io.{BufferedWriter, FileWriter, PrintWriter}
import lomrf.logic.{AtomSignature, AtomicFormula, DefiniteClauseConstruct, Formula}
import lomrf.mln.model._
import scala.collection.mutable
import scala.util.Try

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
