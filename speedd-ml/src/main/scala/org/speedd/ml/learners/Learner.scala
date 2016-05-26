package org.speedd.ml.learners

import auxlib.log.Logging

trait Learner extends Logging {
  def trainFor(startTs: Int, endTs: Int, batchSize: Int, excludeInterval: Option[(Int, Int)] = None): Unit
}
