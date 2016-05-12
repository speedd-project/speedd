package org.speedd.ml.loaders

import auxlib.log.Logging
import lomrf.logic.{AtomSignature, Clause}
import lomrf.mln.model._

sealed trait Batch

trait BatchLoader extends Logging {

  def forInterval(startTs: Int, endTs: Int): Unit //Batch

}

/**
  * This class holds the data of a single (micro) training batch
  *
  * @param mlnSchema the MLN schema definition for LoMRF
  * @param trainingEvidence the training evidence of this batch (i.e., ground input predicates and ground annotation predicates)
  * @param nonEvidenceAtoms the set of atomic signatures of all non-evidence atoms in the given training evidence (i.e., annotation predicates)
  * @param clauses a collection of clauses in Clausal Normal Form
  */
case class TrainingBatch(mlnSchema: MLNSchema,
                         trainingEvidence: Evidence,
                         nonEvidenceAtoms: Set[AtomSignature],
                         clauses: Vector[Clause]) extends Batch


/**
  * This class holds the data of a single (micro) inference batch
  *
  * @param mlnSchema the MLN schema definition for LoMRF
  * @param evidence the evidence of this batch (i.e., ground input predicates)
  * @param annotation the annotation of this batch (i.e., ground annotation predicates)
  * @param queryAtoms the set of atomic signatures of all query atoms in the given evidence
  * @param clauses a collection of clauses in Clausal Normal Form
  */
case class InferenceBatch(mlnSchema: MLNSchema,
                          evidence: Evidence,
                          annotation: EvidenceDB,
                          queryAtoms: Set[AtomSignature],
                          clauses: Vector[Clause]) extends Batch