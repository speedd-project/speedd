package org.speedd.ml.loaders

import auxlib.log.Logging
import lomrf.logic.{AtomSignature, Clause, FunctionMapping}
import lomrf.mln.learning.structure.TrainingEvidence
import lomrf.mln.model._
import org.speedd.ml.util.data._
import org.speedd.ml.util.logic._
import scala.util.{Failure, Success}

/**
  * Can be either [[org.speedd.ml.loaders.TrainingBatch]] or [[org.speedd.ml.loaders.InferenceBatch]]
  */
sealed trait Batch

/**
  * Batch loader interface. Should be implemented by all batch loading classes.
  */
trait BatchLoader extends Logging {

  /**
    * Loads a (micro) batch, either training [[org.speedd.ml.loaders.TrainingBatch]] or
    * inference [[org.speedd.ml.loaders.InferenceBatch]], that holds the data for the
    * specified interval. Furthermore, a simulation id can be provided in case of
    * simulation data.
    *
    * @param startTs starting time point
    * @param endTs end time point
    * @param simulationId simulation id (optional)
    *
    * @return a batch [[org.speedd.ml.loaders.Batch]] subclass specified during implementation
    */
  def forInterval(startTs: Int, endTs: Int, simulationId: Option[Int] = None): Batch

  /**
    * Loads a training evidence batch [[lomrf.mln.learning.structure.TrainingEvidence]] that holds
    * the data for the specified interval. Furthermore, a simulation id can be provided in case of
    * simulation data. Should be used only for loading data during structure learning.
    *
    * @param startTs starting time point
    * @param endTs end time point
    * @param simulationId simulation id (optional)
    *
    * @return a training evidence batch [[lomrf.mln.learning.structure.TrainingEvidence]]
    */
  def forIntervalSL(startTs: Int, endTs: Int, simulationId: Option[Int] = None): TrainingEvidence

  /**
    * Creates the constant domain, function mappings and loads the annotation tuples
    * from the database for the specified interval. Furthermore, a simulation id can be
    * provided in case of simulation data.
    *
    * @example
    *          Loader function should have the following signature:<br><br>
    *          ``def loaderSymbol(simulationId, startTs, endTs, anyConstantDomain): (DomainMap, AnnotationTuples)``
    *
    * @param kbConstants a constant domain containing the KB constants
    * @param functionSchema a function schema
    * @param startTs start time point
    * @param endTs end time point
    * @param simulationId simulation id (optional)
    * @param loader a loader function that loads the domain and the annotation tuples from the database. The loader
    *               function should take the arguments simulation id, start time, end time and constant domain.
    *
    * @return a tuple containing the constant domain, function mappings and annotation tuples
    */
  protected def loadAll[A, B, C, D](kbConstants: ConstantsDomain, functionSchema: FunctionSchema,
                                    startTs: Int, endTs: Int , simulationId: Option[Int] = None,
                                    loader: (Option[Int], Int, Int, ConstantsDomain) => (DomainMap, AnnotationTuples[A, B, C, D])):
                                    (ConstantsDomain, Map[AtomSignature, Iterable[FunctionMapping]], AnnotationTuples[A, B, C, D]) = {

    // Load domains mappings and annotation tuples
    val (domainsMap, annotationTuples) = loader(simulationId, startTs, endTs, kbConstants)

    // Generate function mappings
    val (functionMappings, generatedDomainMap) = {
      generateFunctionMappings(functionSchema, domainsMap) match {
        case Success(result) => result
        case Failure(exception) => fatal("Failed to generate function mappings", exception)
      }
    }

    // Create constant domain builder and append all found constants for each domain
    val constantsDomainBuilder = ConstantsDomainBuilder.from(kbConstants)

    for ((name, symbols) <- domainsMap)
      constantsDomainBuilder ++=(name, symbols)

    for ((name, symbols) <- generatedDomainMap)
      constantsDomainBuilder ++=(name, symbols)

    val constantsDomain = constantsDomainBuilder.result()

    debug(s"Domains:\n${constantsDomain.map(a => s"${a._1} -> [${a._2.mkString(", ")}]").mkString("\n\n")}")

    (constantsDomain, functionMappings, annotationTuples)
  }
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
                         annotation: EvidenceDB,
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