package org.speedd.ml.inference

import java.io.File
import auxlib.log.Logging
import lomrf.logic.AtomSignature
import lomrf.mln.grounding.MRFBuilder
import lomrf.mln.inference._
import lomrf.mln.model._
import org.speedd.ml.loaders.cnrs.collected.InferenceBatchLoader
import org.speedd.ml.util.logic.{Atom2SQLParser, AtomMapping}
import lomrf.util.evaluation._

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

    var results = Vector[EvaluationStats]()

    for ( ((currStartTime, currEndTime), idx) <- microIntervals.zipWithIndex) {
      info(s"Loading micro-batch training data no. $idx, for the temporal interval [$currStartTime, $currEndTime]")
      val batch = batchLoader.forInterval(currStartTime, currEndTime)

      info {
        s"""
           |${batch.evidence.constants.map(e => s"Domain '${e._1}' contains '${e._2.size}' constants.").mkString("\n")}
           |Total number of 'True' evidence atom instances: ${batch.evidence.db.values.map(_.numberOfTrue).sum}
           |Total number of 'True' non-evidence atom instances: ${batch.annotation.values.map(_.numberOfTrue).sum}
          """.stripMargin
      }

      val domainSpace = PredicateSpace(batch.mlnSchema, queryAtoms, batch.evidence.constants)

      val mln = new MLN(batch.mlnSchema, domainSpace, batch.evidence, batch.clauses)
      info(mln.toString())

      info("Creating MRF...")
      val mrfBuilder = new MRFBuilder(mln, createDependencyMap = true)
      val mrf = mrfBuilder.buildNetwork

      val state = new ILP(mrf).infer()

      val atoms = (state.mrf.queryAtomStartID until state.mrf.queryAtomEndID)
        .map(id => state.mrf.fetchAtom(id)).par

      val result = Evaluate(atoms, batch.annotation)(mln)
      println("RESULT: " + result)
      results +:= result

      val r = (results.map(_._1).sum, results.map(_._2).sum, results.map(_._3).sum, results.map(_._4).sum)
      println(r)
      println(Metrics.f1(r._1, r._3, r._4))

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