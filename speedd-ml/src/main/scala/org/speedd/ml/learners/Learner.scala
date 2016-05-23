package org.speedd.ml.learners

import auxlib.log.Logging

trait Learner extends Logging {
  def trainFor(startTs: Int, endTs: Int, batchSize: Int): Unit
}
