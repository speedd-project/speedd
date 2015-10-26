package org.speedd.ml.model

import lomrf.logic.{Clause, AtomSignature}
import lomrf.mln.model.{Evidence, MLNSchema}


// 1. val mlnSchema = MLNSchema(predicateSchema, kb.functionSchema, kb.dynamicPredicates, kb.dynamicFunctions)
// 2. val trainingEvidence = trainingDB.result()
// 3. nonEvidenceAtoms
// 4. clauses
case class TrainingBatch(mlnSchema: MLNSchema, trainingEvidence: Evidence, nonEvidenceAtoms: Set[AtomSignature], clauses: Vector[Clause])
