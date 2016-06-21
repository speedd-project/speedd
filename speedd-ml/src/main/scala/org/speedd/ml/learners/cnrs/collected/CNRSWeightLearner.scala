package org.speedd.ml.learners.cnrs.collected

import java.io.{File, PrintStream}
import auxlib.log.Logging
import lomrf.app.Algorithm
import lomrf.logic.AtomSignature
import lomrf.mln.grounding.MRFBuilder
import lomrf.mln.inference.Solver
import lomrf.mln.learning.weight.OnlineLearner
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

    println(microIntervals)
    info(s"Number of micro-intervals: ${microIntervals.size}")

    var learner: OnlineLearner = null

    for ( ((currStartTime, currEndTime), idx) <- microIntervals.zipWithIndex) {
      info(s"Loading micro-batch training data no. $idx, for the temporal interval [$currStartTime, $currEndTime]")
      val batch = batchLoader.forInterval(currStartTime, currEndTime)

      val domainSpace = PredicateSpace(batch.mlnSchema, nonEvidenceAtoms, batch.trainingEvidence.constants)

      val evidenceAtoms = predicateSchema.keySet -- nonEvidenceAtoms

      // Partition the training data into annotation and evidence databases
      var (annotationDB, atomStateDB) = batch.trainingEvidence.db.partition(e => nonEvidenceAtoms.contains(e._1))

      // Show stats for the current batch
      info {
        s"""
           |${batch.trainingEvidence.constants.map(e => s"Domain '${e._1}' contains '${e._2.size}' constants.").mkString("\n")}
           |Total number of 'True' evidence atom instances: ${atomStateDB.values.map(_.numberOfTrue).sum}
           |Total number of 'True' non-evidence atom instances: ${annotationDB.values.map(_.numberOfTrue).sum}
          """.stripMargin
      }

      // Define all non evidence atoms as unknown in the evidence database
      for (signature <- annotationDB.keysIterator)
        atomStateDB += (signature -> AtomEvidenceDB.allUnknown(domainSpace.identities(signature)))

      // Define all non evidence atoms for which annotation was not given as false in the annotation database (close world assumption)
      for (signature <- nonEvidenceAtoms; if !annotationDB.contains(signature)) {
        warn(s"Annotation was not given in the training data for predicate '$signature', assuming FALSE state for all its groundings.")
        annotationDB += (signature -> AtomEvidenceDB.allFalse(domainSpace.identities(signature)))
      }

      for (signature <- kb.predicateSchema.keysIterator; if !atomStateDB.contains(signature)) {
        if (evidenceAtoms.contains(signature))
          atomStateDB += (signature -> AtomEvidenceDB.allFalse(domainSpace.identities(signature)))
      }

      val evidence = new Evidence(batch.trainingEvidence.constants, atomStateDB, batch.trainingEvidence.functionMappers)

      val mln = new MLN(batch.mlnSchema, domainSpace, evidence, batch.clauses)
      info(mln.toString())

      info("Creating MRF...")
      val mrfBuilder = new MRFBuilder(mln, createDependencyMap = true)
      val mrf = mrfBuilder.buildNetwork

      if(idx == 0) learner = new OnlineLearner(mln, algorithm = Algorithm.ADAGRAD_FB, lossAugmented = true,
        printLearnedWeightsPerIteration = true, ilpSolver = Solver.LPSOLVE)
      learner.learningStep(idx + 1, mrf, annotationDB)
    }

    info("Weight learning is complete!")

    learner.writeResults(new PrintStream(outputKB))

    info(s"Resulting trained MLN file is written to '${outputKB.getPath}'.")
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
    val (kb, kbConstants) = KB.fromFile(inputKB.getPath)


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