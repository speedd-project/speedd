package org.speedd.ml.inference

import java.io.File
import auxlib.log.Logging
import lomrf.logic.AtomSignature
import lomrf.mln.model._
import org.speedd.ml.loaders.cnrs.collected.InferenceBatchLoader
import org.speedd.ml.util.logic.{Atom2SQLParser, AtomMapping}

final class CNRSInferenceEngine private(kb: KB,
                                        kbConstants: ConstantsDomain,
                                        predicateSchema: PredicateSchema,
                                        atomMappings: List[AtomMapping],
                                        inputKB: File,
                                        queryAtoms: Set[AtomSignature]) extends InferenceEngine with Logging {

  private lazy val batchLoader = new InferenceBatchLoader(kb, kbConstants, predicateSchema, queryAtoms, atomMappings)

  override def inferFor(startTs: Int, endTs: Int, batchSize: Int): Unit = {

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

object CNRSInferenceEngine extends Logging {

  val DEFAULT_NON_EVIDENCE_ATOMS = Set[AtomSignature](AtomSignature("HoldsAt", 2))

  def apply(inputKB: File,
            atomMappingsFile: File,
            queryAtoms: Set[AtomSignature] = DEFAULT_NON_EVIDENCE_ATOMS): CNRSInferenceEngine = {

    info(s"Processing the given input KB '${inputKB.getPath}'.")
    val (kb, constantsDomainBuilder) = KB.fromFile(inputKB.getPath)
    val kbConstants = constantsDomainBuilder.result()

    info(s"Processing the given atom mappings '${atomMappingsFile.getPath}'")
    val atomMappings = Atom2SQLParser.parseFrom(atomMappingsFile)

    whenDebug {
      debug(atomMappings.mkString("\n"))
    }

    println(kb.formulas.map(_.toText))
    new CNRSInferenceEngine(kb, kbConstants, kb.predicateSchema, atomMappings, inputKB, queryAtoms)
  }
}