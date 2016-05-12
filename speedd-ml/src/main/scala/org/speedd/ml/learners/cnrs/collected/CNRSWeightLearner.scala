package org.speedd.ml.learners.cnrs.collected

import java.io.File
import auxlib.log.Logging
import lomrf.logic.AtomSignature
import lomrf.mln.model._
import org.speedd.ml.learners.Learner
import org.speedd.ml.loaders.cnrs.collected.TrainingBatchLoader
import org.speedd.ml.util.logic.RuleTransformation
import scala.util.{Failure, Success}
import org.speedd.ml.util.logic._

final class CNRSWeightLearner private(kb: KB,
                                      kbConstants: ConstantsDomain,
                                      predicateSchema: PredicateSchema,
                                      ruleTransformations: Iterable[RuleTransformation],
                                      inputKB: File,
                                      outputKB: File,
                                      inputSignatures: Set[AtomSignature],
                                      targetSignatures: Set[AtomSignature],
                                      nonEvidenceAtoms: Set[AtomSignature]) extends Learner {

  private lazy val batchLoader = new TrainingBatchLoader(kb, kbConstants, predicateSchema, nonEvidenceAtoms, ruleTransformations)

  override def trainFor(startTs: Int, endTs: Int, batchSize: Int): Unit = {

    val range = startTs to endTs by batchSize
    val intervals = if (!range.contains(endTs)) range :+ endTs else range

    val microIntervals = intervals.sliding(2).map(i => (i.head, i.last)).toList
    info(s"Number of micro-intervals: ${microIntervals.size}")

    for ( ((currStartTime, currEndTime), idx) <- microIntervals.zipWithIndex) {
      info(s"Loading micro-batch training data no. $idx, for the temporal interval [$currStartTime, $currEndTime]")
      batchLoader.forInterval(currStartTime, currEndTime)
    }
  }

}

object CNRSWeightLearner extends Logging {

  val DEFAULT_NON_EVIDENCE_ATOMS = Set[AtomSignature](AtomSignature("HoldsAt", 2))

  def apply(inputKB: File,
            outputKB: File,
            inputSignatures: Set[AtomSignature],
            targetSignatures: Set[AtomSignature],
            nonEvidenceAtoms: Set[AtomSignature] = DEFAULT_NON_EVIDENCE_ATOMS): CNRSWeightLearner = {
    // ---
    // --- Prepare the KB:
    // ---
    // Attempt to simplify the given KB --- i.e., exploit input data, in order to eliminate existentially
    // quantified variables that may appear in the body part of each definite clause
    info(s"Processing the given input KB '${inputKB.getPath}', in order to make simplifications.")
    val (kb, constantsDomainBuilder) = KB.fromFile(inputKB.getPath)
    val kbConstants = constantsDomainBuilder.result()


    // Try to simplify the knowledge base, in order to eliminate existential quantifiers.
    val (ruleTransformations, predicateSchema) = kb
      .simplify(inputSignatures, targetSignatures, kbConstants)
      .getOrElse(fatal(s"Failed to transform the given knowledge base '${inputKB.getPath}'"))


    // Output all transformations into a log file
    exportTransformations(ruleTransformations, outputKB.getPath + "_rule_transformations.log") match {
      case Success(path) => info(s"KB transformations are written into '$path'")
      case Failure(ex) => error(s"Failed to write KB transformations", ex)
    }

    new CNRSWeightLearner(
      kb, kbConstants, predicateSchema, ruleTransformations, inputKB,
      outputKB, inputSignatures, targetSignatures, nonEvidenceAtoms)
  }
}