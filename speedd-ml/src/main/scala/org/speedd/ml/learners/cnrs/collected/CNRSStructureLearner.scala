package org.speedd.ml.learners.cnrs.collected

import java.io.{File, PrintStream}
import auxlib.log.Logging
import lomrf.logic.AtomSignature
import lomrf.mln.learning.structure.{OSLa, TrainingEvidence}
import lomrf.mln.model._
import org.speedd.ml.learners.Learner
import org.speedd.ml.loaders.cnrs.collected.StructureTrainingBatchLoader
import org.speedd.ml.util.logic._

final class CNRSStructureLearner private(kb: KB,
                                         kbConstants: ConstantsDomain,
                                         predicateSchema: PredicateSchema,
                                         atomMappings: List[TermMapping],
                                         inputKB: File,
                                         outputKB: File,
                                         inputSignatures: Set[AtomSignature],
                                         targetSignatures: Set[AtomSignature],
                                         nonEvidenceAtoms: Set[AtomSignature],
                                         modes: ModeDeclarations,
                                         maxLength: Int,
                                         threshold: Int) extends Learner {

  private lazy val batchLoader = new StructureTrainingBatchLoader(kb, kbConstants, predicateSchema, nonEvidenceAtoms, inputSignatures, atomMappings)

  private lazy val learner = OSLa(kb, kbConstants, nonEvidenceAtoms, targetSignatures, modes, maxLength, allowFreeVariables = false, threshold)

  override def trainFor(startTs: Int, endTs: Int, batchSize: Int, excludeInterval: Option[(Int, Int)] = None): Unit = {

    val microIntervals =
      if (excludeInterval.isDefined) {
        info(s"Excluding interval ${excludeInterval.get} from $startTs,$endTs")
        val (excludeStart, excludeEnd) = excludeInterval.get
        val range1 = startTs until excludeStart by batchSize
        val range2 = excludeEnd to endTs by batchSize
        val intervals = if (excludeEnd < endTs) range2 :+ endTs else range2

        range1.sliding(2).map(i => (i.head, i.last)).toList ++
          intervals.sliding(2).map(i => (i.head, i.last)).toList
      }
      else {
        val range = startTs to endTs by batchSize
        val intervals = if (!range.contains(endTs)) range :+ endTs else range
        intervals.sliding(2).map(i => (i.head, i.last)).toList
      }

    info(s"Number of micro-intervals: ${microIntervals.size}")

    for ( ((currStartTime, currEndTime), idx) <- microIntervals.zipWithIndex) {
      info(s"Loading micro-batch training data no. $idx, for the temporal interval [$currStartTime, $currEndTime]")
      val batch = batchLoader.forInterval(currStartTime, currEndTime)

      learner.reviseTheory(batch)
    }

    info("Structure learning is complete!")

    learner.writeResults(new PrintStream(outputKB))

    info(s"Resulting trained MLN file is written to '${outputKB.getPath}'.")
  }
}

object CNRSStructureLearner extends Logging {

  val DEFAULT_NON_EVIDENCE_ATOMS = Set[AtomSignature](AtomSignature("HoldsAt", 2))

  def apply(inputKB: File,
            outputKB: File,
            sqlFunctionMappingsFile: File,
            modes: ModeDeclarations,
            maxLength: Int,
            threshold: Int,
            inputSignatures: Set[AtomSignature],
            targetSignatures: Set[AtomSignature],
            nonEvidenceAtoms: Set[AtomSignature] = DEFAULT_NON_EVIDENCE_ATOMS): CNRSStructureLearner = {
    // ---
    // --- Prepare the KB:
    // ---
    // Attempt to simplify the given KB --- i.e., exploit input data, in order to eliminate existentially
    // quantified variables that may appear in the body part of each definite clause
    info(s"Processing the given input KB '${inputKB.getPath}', in order to make simplifications.")
    val (kb, kbConstants) = KB.fromFile(inputKB.getPath, convertFunctions = true)

    info(s"Processing the given atom mappings '${sqlFunctionMappingsFile.getPath}'")
    val sqlFunctionMappings = Term2SQLParser.parseFunctionTermFrom(sqlFunctionMappingsFile)

    whenDebug {
      debug(sqlFunctionMappings.mkString("\n"))
    }

    new CNRSStructureLearner(
      kb, kbConstants, kb.predicateSchema, sqlFunctionMappings, inputKB,
      outputKB, inputSignatures, targetSignatures, nonEvidenceAtoms, modes, maxLength, threshold)
  }
}