package org.speedd.ml.util.logic

import lomrf.logic.{AtomSignature, WeightedDefiniteClause}

class RuleTransformation private(val originalRule: WeightedDefiniteClause,
                                 val atomMappings: Map[DerivedAtom, String],
                                 val schema: Map[AtomSignature, Vector[String]],
                                 val transformedRule: WeightedDefiniteClause) extends Serializable {

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

