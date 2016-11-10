package org.speedd.ml.loaders.cnrs.simulation.highway

import java.io.File
import org.speedd.ml.loaders.DataLoader
import org.speedd.ml.model.cnrs.simulation.highway.{Annotation, AnnotationData}
import org.speedd.ml.util.data._
import org.speedd.ml.util.data.DatabaseManager._
import slick.driver.PostgresDriver.api._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

/**
  * Loads and converts annotation highway data from CSV files. The data was simulated by CNRS.
  *
  * <p>
  * The expected format of the CSV file is the following:
  * <ul>
  *   <li>simulation number: simulation number (integer)</li>
  *   <li>section id: a unique id of a particular section</li>
  *   <li>duration: represents the duration of the incident</li>
  *   <li>start time: start time of the incident</li>
  *   <li>length of the effected area: the length of road that the incident takes place</li>
  * </ul>
  * </p>
  */
object AnnotationDataLoader extends DataLoader {

  private val DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss"
  private val DATE_TIME_FORMAT_SHORT = "yy-MM-dd HH:mm"
  //private val INIT_TIME = ts2UnixTS("18:00", DATE_TIME_FORMAT, DATE_TIME_FORMAT_SHORT)

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
    val start = ts2UnixTS("2015-12-16 " + source(3), DATE_TIME_FORMAT, DATE_TIME_FORMAT_SHORT, startTs = 1450281615, round = true)
    Some(Annotation(source(0).toInt, source(1).toInt, start,
      start + duration2Seconds(source(2)) / 15, source(4).toDouble, "traffic_jam"))
  }

}
