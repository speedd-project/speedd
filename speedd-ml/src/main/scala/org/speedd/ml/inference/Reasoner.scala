package org.speedd.ml.inference

import auxlib.log.Logging
import org.speedd.ml.util.data._

/**
  * Reasoner interface. Should be implemented by all reasoning engine classes.
  */
trait Reasoner extends Logging {

  /**
    * Perform inference in the MLN model for the specified interval using the given batch size. Furthermore,
    * a set of simulation id can be provided in case of simulation data. The inference is performed in steps,
    * were each step is considered a test case.
    *
    * @param startTs start time point
    * @param endTs end time point
    * @param batchSize batch size for each inference step
    * @param useOnlyConstants a subset of constant domain to be used (optional)
    * @param simulationIds a set of simulation ids to be used for inference
    */
  def inferFor(startTs: Int, endTs: Int, batchSize: Int,
               useOnlyConstants: Option[DomainMap] = None,
               simulationIds: List[Int] = List.empty): Unit
}
