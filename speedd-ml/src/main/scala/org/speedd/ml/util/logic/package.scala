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

package org.speedd.ml.util

import java.io._

import lomrf.logic._
import lomrf.mln.model._
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.{SQLContext, DataFrame}
import org.speedd.ml.util.data._
import scala.collection.mutable
import scala.util.{Failure, Success, Try}

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

  def unionDomains(initial: ConstantsDomain = Map.empty,
                   static: List[(String, Iterable[String])] = List.empty,
                   df: List[DataFrame] = List.empty,
                   aliases: Map[String, String] = Map.empty)(implicit sc: SparkContext, sqlContext: SQLContext): Map[String, RDD[String]] = {

    import sqlContext.implicits._

    var listDF: List[DataFrame] = df

    for ((name, set) <- initial)
      listDF = sc.parallelize(set.toSeq).toDF(name) :: listDF

    for ((name, set) <- static)
      listDF = sc.parallelize(set.toSeq).toDF(name) :: listDF

    symbolsPerColumn(listDF: _*)(aliases)
  }


  def exportTransformations(ruleTransformations: Iterable[RuleTransformation],
                            outputPath: String,
                            interval: Option[(Long, Long)] = None) = Try[String]{

    val p = new PrintWriter(new BufferedWriter(new FileWriter(outputPath, false))) // overwrite file, if exists

    interval match{
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

  def generateFunctionMappings(functionSchema: FunctionSchema, domainMap: Map[String, RDD[String]], domainAliases: Map[String, String] = Map.empty)
                              (implicit sc: SparkContext, sqlContext: SQLContext): Try[(Seq[(AtomSignature, RDD[FunctionMapping])], List[DataFrame])] = {

    import sqlContext.implicits._

    // ---
    // --- Compute function mappings
    // ---
    val functionMappings = new Array[(AtomSignature, RDD[FunctionMapping])](functionSchema.size)

    var generatedDomains: List[DataFrame] = Nil

    for (((signature, (retDomain, argDomains)), index) <- functionSchema.zipWithIndex.par) {
      val symbol = signature.symbol

      /*if (domainMap.contains(retDomain))
        fatal(s"Cannot reassign domain '$retDomain' using function mappings.")*/

      val argDomainsDF = argDomains.map { name =>
        val colName = domainAliases.getOrElse(name, name)
        domainMap.getOrElse(colName, return Failure(new NoSuchElementException(s"Unknown domain name '$colName'"))).toDF(name)
      }

      val products = argDomainsDF.reduceLeft((a, b) => a.repartition(1).join(b.repartition(1)))
        .map(r => (0 until r.length).map(i => Constant(r.get(i).toString)))
        .zipWithUniqueId()
        .map {
          case (constants, uid) =>
            val retConstant = s"r_${index}_$uid"
            retConstant -> FunctionMapping(retConstant, symbol, constants.toVector)
        }.cache()

      generatedDomains = products.keys.toDF(retDomain) :: generatedDomains

      functionMappings(index) = signature -> products.values
    }

    Success((functionMappings.toSeq, generatedDomains))
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
   *
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
