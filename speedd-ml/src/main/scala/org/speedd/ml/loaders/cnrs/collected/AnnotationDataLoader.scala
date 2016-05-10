package org.speedd.ml.loaders.cnrs.collected

import java.io.File
import java.text.SimpleDateFormat
import org.speedd.ml.loaders.DataLoader
import org.speedd.ml.model.cnrs.collected.{Annotation, annotation}
import org.speedd.ml.util.data.CSV
import org.speedd.ml.util.data.DatabaseManager._
import slick.driver.PostgresDriver.api._
import scala.concurrent.ExecutionContext.Implicits.global

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
  *   <li>state: incident state (we are interested for `Clos`)</li>
  *   <li>location: string indicating the location of the incident</li>
  *   <li>sensor: sensor number/type</li>
  *   <li>description: the annotation (name of the incident)</li>
  * </ul>
  * </p>
  */
object AnnotationDataLoader extends DataLoader {

  private val START = 1396299600L

  private val validSeconds = Vector(0, 15, 30, 45)

  private val DATE_TIME_FORMAT = "dd/MM/yyyy HH:mm:ss"
  private val DATE_TIME_FORMAT_SHORT = "dd/MM/yy HH:mm"

  private val PR_LOCATION = """^[\d\w\s]+PR(\d+)\+(\d+)[-\d\w\s]+PR(\d+)\+(\d+)$""".r

  private val ts2UnixTS = (dateTime: String) => {

    val components = dateTime.split(":")
    val seconds = components.last.toInt

    val roundedTime =
      if (dateTime.trim().length == 19 && !validSeconds.contains(seconds)) {
        val replacement = validSeconds.minBy(v => math.abs(v - seconds.toInt))
        var result = StringBuilder.newBuilder
        for (i <- 0 until components.length - 1) result ++= components(i) + ":"
        if (replacement == 0) result ++= "0"
        result ++= replacement.toString
        result.result()
      }
      else dateTime

    val dtFormat = new SimpleDateFormat(if(roundedTime.trim.length == 19) DATE_TIME_FORMAT else DATE_TIME_FORMAT_SHORT )
    val step = (dtFormat.parse(roundedTime).getTime / 1000 - START)/15
    (START + step).toInt
  }

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

  override def loadAll(inputFiles: Seq[File]) = {
    info("Loading annotation data")

    val results = inputFiles.filter(f => f.isFile && f.canRead).
      flatMap { file =>
        info(s"Parsing file '${file.getName}'")
        CSV.parse[Annotation](file, toAnnotation)
      }

    exec {
      annotation.createSchema() andThen
        (annotation ++= results)
    }.onSuccess { case s => info("Done!") }
  }

  private def toAnnotation(source: Array[String]): Option[Annotation] = {
    parseLocation(source(5)) match {
      case Some((startLocation, endLocation)) =>
        Some(Annotation(ts2UnixTS(source(2)), ts2UnixTS(source(3)), source(1).toInt,
          source(7), source(6).toInt, startLocation, endLocation))
      case _ => None
    }
  }

}
