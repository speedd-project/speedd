package org.speedd.ml.learners.cnrs.collected

import java.io.File
import auxlib.log.Logging
import lomrf.logic.AtomSignature
import lomrf.mln.model._
import org.speedd.ml.learners.Learner
import org.speedd.ml.util.logic._

final class CNRSStructureLearner private(kb: KB,
                                         kbConstants: ConstantsDomain,
                                         predicateSchema: PredicateSchema,
                                         atomMappings: List[AtomMapping],
                                         inputKB: File,
                                         queryAtoms: Set[AtomSignature]) extends Learner {

  override def trainFor(startTs: Int, endTs: Int, batchSize: Int, excludeInterval: Option[(Int, Int)] = None): Unit = {
    ???
  }
}

object CNRSStructureLearner extends Logging {

  val DEFAULT_NON_EVIDENCE_ATOMS = Set[AtomSignature](AtomSignature("HoldsAt", 2))

  def apply(inputKB: File,
            outputKB: File,
            functionMappingsFile: File,
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

    info(s"Processing the given atom mappings '${functionMappingsFile.getPath}'")
    val functionMappings = Atom2SQLParser.parseFrom(functionMappingsFile)

    whenDebug {
      debug(functionMappings.mkString("\n"))
    }

    println(kb.formulas.map(_.toText))
    new CNRSStructureLearner(kb, kbConstants, kb.predicateSchema, functionMappings, inputKB, nonEvidenceAtoms)
  }
}