package org.speedd.ml.learners.cnrs.simulation.highway

import java.io.{File, PrintStream}

import auxlib.log.Logging
import lomrf.app.Algorithm
import lomrf.logic.AtomSignature
import lomrf.mln.grounding.MRFBuilder
import lomrf.mln.inference.Solver
import lomrf.mln.learning.weight.OnlineLearner
import lomrf.mln.model._
import org.speedd.ml.learners.Learner
import org.speedd.ml.loaders.TrainingBatch
import org.speedd.ml.loaders.cnrs.simulation.highway.TrainingBatchLoader
import org.speedd.ml.util.logic.{Term2SQLParser, TermMapping}

final class WeightLearner private(kb: KB,
                                  kbConstants: ConstantsDomain,
                                  predicateSchema: PredicateSchema,
                                  termMappings: List[TermMapping],
                                  inputKB: File,
                                  outputKB: File,
                                  inputSignatures: Set[AtomSignature],
                                  targetSignatures: Set[AtomSignature],
                                  nonEvidenceAtoms: Set[AtomSignature]) extends Learner {

  private lazy val batchLoader =
    new TrainingBatchLoader(kb, kbConstants, predicateSchema, inputSignatures, nonEvidenceAtoms, termMappings)

  /**
    * Train the MLN model for the specified interval using the given batch size. Furthermore,
    * a specific interval can be excluded (optional) from the training sequence in order to be
    * used for testing. Finally, a set of simulation id can be provided in case of simulation data.
    *
    * @param startTs start time point
    * @param endTs end time point
    * @param batchSize batch size for each learning step
    * @param excludeInterval interval to be excluded from the training sequence (optional)
    * @param simulationIds a set of simulation ids to be used for training
    */
  override def trainFor(startTs: Int, endTs: Int, batchSize: Int,
                        excludeInterval: Option[(Int, Int)] = None, simulationIds: List[Int] = List.empty) = {

    // For all given simulation ids
    for (id <- simulationIds) {
      info(s"Simulation id $id")

      val microIntervals =
        if (excludeInterval.isDefined) {
          info(s"Excluding interval ${excludeInterval.get} from $startTs,$endTs")
          val (excludeStart, excludeEnd) = excludeInterval.get
          val range1 = startTs to excludeStart by batchSize
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

      var learner: OnlineLearner = null

      for ( ((currStartTime, currEndTime), idx) <- microIntervals.zipWithIndex) {
        info(s"Loading micro-batch training data no. $idx, for the temporal interval [$currStartTime, $currEndTime]")
        val batch: TrainingBatch = batchLoader.forInterval(currStartTime, currEndTime, Some(id))

        val domainSpace = PredicateSpace(batch.mlnSchema, nonEvidenceAtoms, batch.trainingEvidence.constants)

        // Statistics for the current batch
        info {
          s"""
             |${batch.trainingEvidence.constants.map(e => s"Domain '${e._1}' contains '${e._2.size}' constants.").mkString("\n")}
             |Total number of 'True' evidence atom instances: ${batch.trainingEvidence.db.values.map(_.numberOfTrue).sum}
             |Total number of 'True' non-evidence atom instances: ${batch.annotation.values.map(_.numberOfTrue).sum}
          """.stripMargin
        }

        val mln = new MLN(batch.mlnSchema, domainSpace, batch.trainingEvidence, batch.clauses)
        info(mln.toString())

        info("Creating MRF...")
        val mrfBuilder = new MRFBuilder(mln, createDependencyMap = true)
        val mrf = mrfBuilder.buildNetwork

        if(idx == 0) learner =
          new OnlineLearner(mln, algorithm = Algorithm.ADAGRAD_FB,
                            lossAugmented = true, printLearnedWeightsPerIteration = true,
                            ilpSolver = Solver.LPSOLVE)

        learner.learningStep(idx + 1, mrf, batch.annotation)
      }

      learner.writeResults(new PrintStream(outputKB))

      info(s"Resulting trained MLN file is written to '${outputKB.getPath}'.")
    }
  }

}

object WeightLearner extends Logging {

  def apply(inputKB: File,
            outputKB: File,
            sqlFunctionMappingsFile: File,
            inputSignatures: Set[AtomSignature],
            targetSignatures: Set[AtomSignature],
            nonEvidenceAtoms: Set[AtomSignature]): WeightLearner = {
    // ---
    // --- Prepare the KB:
    // ---
    info(s"Processing the given input KB '${inputKB.getPath}'.")
    val (kb, kbConstants) = KB.fromFile(inputKB.getPath)

    // ---
    // --- Parse term mappings to SQL queries:
    // ---
    info(s"Processing the given term mappings '${sqlFunctionMappingsFile.getPath}'")
    val sqlFunctionMappings = Term2SQLParser.parseFunctionTermFrom(sqlFunctionMappingsFile)

    whenDebug {
      debug(sqlFunctionMappings.mkString("\n"))
    }

    new WeightLearner(
      kb, kbConstants, kb.predicateSchema, sqlFunctionMappings, inputKB,
      outputKB, inputSignatures, targetSignatures, nonEvidenceAtoms)
  }
}
