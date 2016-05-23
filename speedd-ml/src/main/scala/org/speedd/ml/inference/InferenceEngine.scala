package org.speedd.ml.inference

trait InferenceEngine {
  def inferFor(startTs: Int, endTs: Int, batchSize: Int): Unit
}
