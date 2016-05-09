package org.speedd.ml.loaders

import java.io.File
import auxlib.log.Logging

/**
  * Data loader interface. Should be implemented by all data loading classes.
  */
trait DataLoader extends Logging {

  def loadAll(inputFiles: Seq[File]): Unit

}

