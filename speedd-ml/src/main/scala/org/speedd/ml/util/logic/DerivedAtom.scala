package org.speedd.ml.util.logic

import lomrf.logic.{AtomicFormula, Variable}

class DerivedAtom(override val symbol: String, override val terms: Vector[Variable])
  extends AtomicFormula(symbol, terms)
