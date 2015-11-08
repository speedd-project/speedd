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

package org.speedd.ml.util.logic

import lomrf.logic.{AtomSignature, WeightedDefiniteClause}

class RuleTransformation private(val originalRule: WeightedDefiniteClause,
                                  val atomMappings: Map[DerivedAtom, String],
                                  val schema: Map[AtomSignature, Vector[String]],
                                  val transformedRule: WeightedDefiniteClause) extends Serializable{

  lazy val isSimplified = originalRule == transformedRule

}


object RuleTransformation {

  def apply(originalRule: WeightedDefiniteClause,
            atomMappings: Map[DerivedAtom, String],
            schema: Map[AtomSignature, Vector[String]]): RuleTransformation = {
    new RuleTransformation(originalRule, atomMappings, schema, originalRule)
  }

  def apply(originalRule: WeightedDefiniteClause,
            atomMappings: Map[DerivedAtom, String],
            schema: Map[AtomSignature, Vector[String]],
            transformedRule: WeightedDefiniteClause): RuleTransformation = {

    if(originalRule == transformedRule) new RuleTransformation(originalRule, atomMappings, schema, originalRule)
    else new RuleTransformation(originalRule, atomMappings, schema, transformedRule)

  }
}