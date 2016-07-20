package org.speedd.ml.learners

import auxlib.log.Logging

/**
  * Learner interface. Should be implemented by all learner classes.
  */
trait Learner extends Logging {

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
  def trainFor(startTs: Int, endTs: Int, batchSize: Int,
               excludeInterval: Option[(Int, Int)] = None, simulationIds: List[Int] = List.empty): Unit

}
