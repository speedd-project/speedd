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
import lomrf.logic.dynamic.DynEqualsBuilder
import lomrf.mln.model._
import scala.collection.mutable
import scala.util.Try

package object logic {

  type PredicateSchema = Map[AtomSignature, Seq[String]]


  implicit class WrappedBody(val body: DefiniteClauseConstruct) extends AnyVal {

    def findAtoms(f: (AtomicFormula) => Boolean): Vector[AtomicFormula] ={
      body match {
        case a: AtomicFormula => if(f(a)) Vector(a) else Vector.empty[AtomicFormula]

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


  def exportTransformations(ruleTransformations: Iterable[RuleTransformation], outputPath: String, interval: Option[(Long, Long)] = None) = Try[String]{

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

}
