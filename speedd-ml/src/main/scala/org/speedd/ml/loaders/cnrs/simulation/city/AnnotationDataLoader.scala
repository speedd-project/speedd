package org.speedd.ml.loaders.cnrs.simulation.city

import java.io.File
import org.speedd.ml.loaders.DataLoader
import org.speedd.ml.model.cnrs.simulation.city.{Annotation, AnnotationData}
import org.speedd.ml.util.data.DatabaseManager._
import slick.driver.PostgresDriver.api._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}
import org.speedd.ml.util.data._

/**
  * Loads and converts annotation city data from CSV files. The data was simulated by CNRS.
  *
  * <p>
  * The expected format of the CSV file is the following:
  * <ul>
  *   <li>level of traffic: string indicating the traffic level (high or low)</li>
  *   <li>simulation number: simulation number (integer)</li>
  *   <li>accident: accident counter in each simulation </li>
  *   <li>location: string indicating the location</li>
  *   <li>section id: a unique id of a particular location</li>
  *   <li>duration: represents the duration of the incident</li>
  *   <li>start time: start time of the incident</li>
  *   <li>length of the effected area: the length of road is blocked due to accident</li>
  * </ul>
  * </p>
  */
object AnnotationDataLoader extends DataLoader {

  private val DATE_TIME_FORMAT = "HH:mm:ss"
  private val DATE_TIME_FORMAT_SHORT = "HH:mm"
  private val INIT_TIME = ts2UnixTS("8:00:00", DATE_TIME_FORMAT, DATE_TIME_FORMAT_SHORT)

  /**
    * Loads all data from a sequence of CSV files into the database.
    *
    * @param inputFiles a sequence of files
    */
  override def loadAll(inputFiles: Seq[File]) = {
    info("Loading annotation data")

    val results = inputFiles.filter(f => f.isFile && f.canRead).
      flatMap { file =>
        info(s"Parsing file '${file.getName}'")
        CSV.parse[Annotation](file, toAnnotation, skipHeader = true) match {
          case Success(result) => result
          case Failure(ex) => fatal(ex.getMessage)
        }
      }

    AnnotationData.createSchema()

    asyncExec {
      AnnotationData ++= results
    }.onSuccess { case s => info("Done!") }
  }

  /**
    * Translator function used to map an array of strings produced by the CSV
    * parser into an `Annotation` object.
    *
    * @param source an array of strings
    * @return an Annotation object
    */
  private def toAnnotation(source: Array[String]): Option[Annotation] = {
    val start = ts2UnixTS(source(6), DATE_TIME_FORMAT, DATE_TIME_FORMAT_SHORT)
    Some(Annotation(source(1).toInt, source(0).toString, source(3).toString, source(4).toInt, (start - INIT_TIME) / 15,
      (start - INIT_TIME + duration2Seconds(source(5))) / 15, source(7).toDouble, "accident"))
  }

}
