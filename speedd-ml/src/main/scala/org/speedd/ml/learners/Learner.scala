package org.speedd.ml.learners

import auxlib.log.Logging

trait Learner extends Logging {
  def trainFor(startTs: Long, endTs: Long, batchSize: Int): Unit
}
