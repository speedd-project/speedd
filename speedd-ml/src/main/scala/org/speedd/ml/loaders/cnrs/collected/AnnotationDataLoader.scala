package org.speedd.ml.loaders.cnrs.collected

import java.io.File
import org.speedd.ml.loaders.DataLoader
import org.speedd.ml.model.cnrs.collected.{Annotation, AnnotationData}
import org.speedd.ml.util.data.CSV
import org.speedd.ml.util.data.DatabaseManager._
import slick.driver.PostgresDriver.api._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}
import org.speedd.ml.util.data._

/**
  * Loads and converts annotation data from CSV files. The data is collected and provided by CNRS.
  *
  * <p>
  * The expected format of the CSV file is the following:
  * <ul>
  *   <li>grouping: grouping number (long)</li>
  *   <li>event_num: event number (integer)</li>
  *   <li>start_time: string indicating the beginning date-time </li>
  *   <li>end_time: string indicating the end date-time</li>
  *   <li>state: incident state</li>
  *   <li>location: string indicating the location of the incident</li>
  *   <li>sensor: sensor number/type</li>
  *   <li>description: the annotation (name of the incident)</li>
  * </ul>
  * </p>
  */
object AnnotationDataLoader extends DataLoader {

  private val DATE_TIME_FORMAT = "dd/MM/yyyy HH:mm:ss"
  private val DATE_TIME_FORMAT_SHORT = "dd/MM/yy HH:mm"

  private val PR_LOCATION = """^[\d\w\s]+PR(\d+)\+(\d+)[-\d\w\s]+PR(\d+)\+(\d+)$""".r

  /**
    * Parse a string and return a starting and ending location
    */
  private val parseLocation = (src: String) => src match {
    case PR_LOCATION(startPr, startDist, endPr, endDist) =>
      val dist = prDistances
      try {
        val start = dist(startPr.toInt) + startDist.toInt
        val end = dist(endPr.toInt) + endDist.toInt

        Some(start, end)
      } catch {
        case e: NumberFormatException =>
          error("Failed to parse location data")
          None
        case e: ArrayIndexOutOfBoundsException =>
          error("Unknown PR point (should be ranging from 0 to 11)")
          None
      }

    case _ => None
  }

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
        CSV.parse[Annotation](file, toAnnotation) match {
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
    parseLocation(source(5)) match {
      case Some((startLocation, endLocation)) =>
        val start = ts2UnixTS(source(2), DATE_TIME_FORMAT, DATE_TIME_FORMAT_SHORT, startTs = 1396299600L, round = true)
        val end = ts2UnixTS(source(3), DATE_TIME_FORMAT, DATE_TIME_FORMAT_SHORT, startTs = 1396299600L, round = true)
        Some(Annotation(start, end, source(1).toInt, source(7),
          source(6).toInt, startLocation, endLocation))
      case _ => None
    }
  }

}
