package org.speedd.ml.inference

import java.io.File
import auxlib.log.Logging
import lomrf.logic.AtomSignature
import lomrf.mln.grounding.MRFBuilder
import lomrf.mln.inference._
import lomrf.mln.model._
import org.speedd.ml.loaders.cnrs.collected.InferenceBatchLoader
import org.speedd.ml.util.logic.{Term2SQLParser, TermMapping}
import lomrf.util.evaluation._

final class CNRSInferenceEngine private(kb: KB,
                                        kbConstants: ConstantsDomain,
                                        predicateSchema: PredicateSchema,
                                        /*atomMappings: List[TermMapping],*/
                                        functionMappings: List[TermMapping],
                                        inputKB: File,
                                        queryAtoms: Set[AtomSignature]) extends InferenceEngine with Logging {

  private lazy val batchLoader = new InferenceBatchLoader(kb, kbConstants, predicateSchema, queryAtoms, /*atomMappings,*/ functionMappings)

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
      results +:= result
    }

    val stats = (results.map(_._1).sum, results.map(_._2).sum, results.map(_._3).sum, results.map(_._4).sum)

    info(s"Statistics: $stats")
    info(s"Precision: ${Metrics.precision(stats._1, stats._3, stats._4)}")
    info(s"Recall: ${Metrics.recall(stats._1, stats._3, stats._4)}")
    info(s"F1: ${Metrics.f1(stats._1, stats._3, stats._4)}")

  }

}

object CNRSInferenceEngine extends Logging {

  val DEFAULT_NON_EVIDENCE_ATOMS = Set[AtomSignature](AtomSignature("HoldsAt", 2))

  def apply(inputKB: File,
            /*atomMappingsFile: File,*/
            functionMappingsFile: File,
            queryAtoms: Set[AtomSignature] = DEFAULT_NON_EVIDENCE_ATOMS): CNRSInferenceEngine = {

    info(s"Processing the given input KB '${inputKB.getPath}'.")
    val (kb, kbConstants) = KB.fromFile(inputKB.getPath)

    //info(s"Processing the given atom mappings '${atomMappingsFile.getPath}'")
    //val atomMappings = Term2SQLParser.parseAtomTermFrom(atomMappingsFile)

    info(s"Processing the given function mappings '${functionMappingsFile.getPath}'")
    val functionMappings = Term2SQLParser.parseFunctionTermFrom(functionMappingsFile)

    //whenDebug {
     // debug(atomMappings.mkString("\n"))
    //}

    println(kb.formulas.map(_.toText))
    new CNRSInferenceEngine(kb, kbConstants, kb.predicateSchema, /*atomMappings,*/ functionMappings, inputKB, queryAtoms)
  }
}