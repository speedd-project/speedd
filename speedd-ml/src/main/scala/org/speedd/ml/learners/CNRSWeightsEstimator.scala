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

package org.speedd.ml.learners

import java.io._
import auxlib.log.Logging
import lomrf.app.Algorithm
import lomrf.logic._
import lomrf.mln.grounding.MRFBuilder
import lomrf.mln.learning.weight.OnlineLearner
import lomrf.mln.model._
import org.apache.spark.SparkContext
import org.apache.spark.sql.SQLContext
import org.speedd.ml.model.cnrs._
import scala.language.implicitConversions
import scala.util.{Failure, Success}
import org.speedd.ml.util.logic._

final class CNRSWeightsEstimator private(kb: KB,
                                         kbConstants: ConstantsDomain,
                                         predicateSchema: PredicateSchema,
                                         ruleTransformations: Iterable[RuleTransformation],
                                         inputKB: File,
                                         outputKB: File,
                                         inputSignatures: Set[AtomSignature],
                                         targetSignatures: Set[AtomSignature],
                                         nonEvidenceAtoms: Set[AtomSignature]) extends WeightEstimator with Logging {

  private lazy val batchLoader = new TrainingBatchLoader(kb, kbConstants, predicateSchema, nonEvidenceAtoms, ruleTransformations)

  override def trainFor(startTime: Long, endTime: Long, batchSize: Int)(implicit sc: SparkContext, sqlContext: SQLContext): Unit = {

    val microIntervals = (startTime to endTime by batchSize).sliding(2).map(i => (i.head, i.last)).toList
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
      info{
        s"""
          |${batch.trainingEvidence.constants.map(e =>s"Domain '${e._1}' contains '${e._2.size}' constants.").mkString("\n")}
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

      /*println("mln.schema.dynamicFunctions:")
      mln.schema.dynamicFunctions.foreach(println)
      println("mln.evidence.functionMappers:")
      mln.evidence.functionMappers.foreach(println)*/

      /*mln.clauses.foreach(println)
      sys.exit()*/

      info("Creating MRF...")
      val mrfBuilder = new MRFBuilder(mln, createDependencyMap = true)
      val mrf = mrfBuilder.buildNetwork

      if(idx == 0) learner = new OnlineLearner(mln, algorithm = Algorithm.ADAGRAD_FB, lossAugmented = true, printLearnedWeightsPerIteration = true)
      learner.learningStep(idx + 1, mrf, annotationDB)

    }

    learner.writeResults(new PrintStream(outputKB))


  }
}

object CNRSWeightsEstimator extends Logging {

  val DEFAULT_NON_EVIDENCE_ATOMS = Set[AtomSignature](AtomSignature("HoldsAt", 2))

  def apply(inputKB: File,
            outputKB: File,
            inputSignatures: Set[AtomSignature],
            targetSignatures: Set[AtomSignature],
            nonEvidenceAtoms: Set[AtomSignature] = DEFAULT_NON_EVIDENCE_ATOMS): CNRSWeightsEstimator = {
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


    new CNRSWeightsEstimator(
      kb, kbConstants, predicateSchema, ruleTransformations, inputKB,
      outputKB, inputSignatures, targetSignatures, nonEvidenceAtoms
    )
  }


}
