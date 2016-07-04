package org.speedd.ml.loaders.cnrs.simulation.highway

import java.io.File
import org.speedd.ml.loaders.DataLoader
import org.speedd.ml.model.cnrs.simulation.highway.{Location, LocationData}
import org.speedd.ml.util.data.CSV
import org.speedd.ml.util.data.DatabaseManager._
import slick.driver.PostgresDriver.api._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

/**
  * Loads and converts location highway data from CSV files. The data was simulated by CNRS.
  *
  * <p>
  * The expected format of the CSV file is the following:
  * <ul>
  *   <li>detector_id: represents the detector id (integer)</li>
  *   <li>section_id: represents the section id (integer)</li>
  * </ul>
  * </p>
  */
object LocationDataLoader extends DataLoader {

  /**
    * Loads all data from a sequence of CSV files into the database.
    *
    * @param inputFiles a sequence of files
    */
  override def loadAll(inputFiles: Seq[File]) = {
    info("Loading location data")

    val results = inputFiles.filter(f => f.isFile && f.canRead).
      flatMap { file =>
        info(s"Parsing file '${file.getName}'")
        CSV.parse[Location](file, toLocation) match {
          case Success(result) => result
          case Failure(ex) => fatal(ex.getMessage)
        }
      }

    LocationData.createSchema()

    asyncExec {
      LocationData ++= results
    }.onSuccess{ case s => info("Done!") }
  }

  /**
    * Translator function used to map an array of strings produced by the CSV
    * parser into an `Location` object.
    *
    * @param source an array of strings
    * @return a `Location` object
    */
  private def toLocation(source: Array[String]): Option[Location] =
    Some(Location(source(0).toInt, source(1).toInt))
}
