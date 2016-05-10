package org.speedd.ml.loaders.cnrs.collected

import java.io.File
import org.speedd.ml.loaders.DataLoader
import org.speedd.ml.model.cnrs.collected.{Location, location}
import org.speedd.ml.util.data.CSV
import org.speedd.ml.util.data.DatabaseManager._
import slick.driver.PostgresDriver.api._
import scala.concurrent.ExecutionContext.Implicits.global

object LocationDataLoader extends DataLoader {

  override def loadAll(inputFiles: Seq[File]) = {
    info("Loading location data")

    val results = inputFiles.filter(f => f.isFile && f.canRead).
      flatMap { file =>
        info(s"Parsing file '${file.getName}'")
        CSV.parse[Location](file, toLocation)
      }

    exec {
      location.createSchema() andThen
        (location ++= results)
    }.onSuccess{ case s => info("Done!") }
  }

  private def toLocation(source: Array[String]): Option[Location] = {

    val prevLaneOpt = source(2) match {
      case x if x != null && x.nonEmpty => Some(x.split(Array('-', ' ', '_')).map(_.trim.capitalize).reduce(_ + _))
      case _ => None
    }

    Some(Location(
      java.lang.Long.valueOf(source(0), 16),
      source(1).split(Array('-', ' ', '_')).map(_.trim.capitalize).reduce(_ + _),
      prevLaneOpt,
      source(3).toDouble,
      source(4).toDouble,
      source(5).toInt,
      source(6).toInt
    ))
  }

}
