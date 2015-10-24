package org.speedd.ml.learners

import org.apache.spark.sql.DataFrame

case class DataSet(evidence: DataFrame, annotation: DataFrame)
