/*
 *  __   ___   ____  ____  ___   ___
 * ( (` | |_) | |_  | |_  | | \ | | \
 * _)_) |_|   |_|__ |_|__ |_|_/ |_|_/
 *
 * SPEEDD project (www.speedd-project.eu)
 * Machine Learning module
 *
 * Copyright (c) Complex Event Recognition Group (cer.iit.demokritos.gr)
 *
 * NCSR Demokritos
 * Institute of Informatics and Telecommunications
 * Software and Knowledge Engineering Laboratory
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with program. If not, see <http://www.gnu.org/licenses/>.
 */

package org.speedd.ml.loaders.cnrs

import java.io.File

import auxlib.log.Logging
import org.apache.spark.SparkContext
import org.apache.spark.sql.{Row, SQLContext}
import org.speedd.ml.loaders.DataLoader
import org.speedd.ml.model.CNRS
import org.speedd.ml.model.CNRS.Annotation
import org.speedd.ml.util.csv._
import scala.language.implicitConversions
import com.datastax.spark.connector._
import java.text.SimpleDateFormat

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
 *   <li>state: incident state (we are interested for 'Clos')</li>
 *   <li>location: string indicating the location of the incident</li>
 *   <li>sens: sensor number/type</li>
 *   <li>description: the annotation (name of the incident)</li>
 * </ul>
 * </p>
 *
 */
object AnnotationDataLoader extends DataLoader with Logging {

  private val DATE_TIME_FORMAT = "dd/MM/yyyy HH:mm:ss"
  private val DATE_TIME_FORMAT_SHORT = "dd/MM/yy HH:mm"

  private val PRLOC = """^[\d\w\s]+PR(\d+)\+(\d+)[-\d\w\s]+PR(\d+)\+(\d+)$""".r

  private val ts2UnixTS = (dateTime: String) => {
    val dtFormat = new SimpleDateFormat(if(dateTime.trim.length ==19 ) DATE_TIME_FORMAT else DATE_TIME_FORMAT_SHORT )
    (dtFormat.parse(dateTime).getTime / 1000).toInt // to take the UNIX time (number of seconds from 1/1/1970 00:00:00)
  }

  private val parseLocation = (src: String) => src match {
    case PRLOC(startPr, startDist, endPr, endDist) =>
      val dist = CNRS.prDistances
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


  private val csvSchema = parseSchema {
    "grouping, event_num:int, start_time, end_time, state, location, sens:int, description"
  }

  override def loadAll(inputFiles: Seq[File])(implicit sc: SparkContext, sqlContext: SQLContext): Unit = {
    import sqlContext.implicits._

    info("Loading annotation data")
    // Annotation Data
    for {
      file <- inputFiles
      if file.isFile && file.canRead
      filename = file.getName} {

      info(s"Parsing file '$filename'")

      // Load input annotation CSV as a dataframe
      val dfAnnotation = sqlContext.read
        .format(CSV_FORMAT)
        .options(CSV_OPTIONS)
        .schema(csvSchema)
        .load(file.getPath)
        .where($"state".eqNullSafe("closed") or $"state".eqNullSafe("Clos")) // take only the rows where state is closed

      // convert to an RDD of Annotation instances and save all of them to Cassandra database
      dfAnnotation.flatMap(toAnnotation).saveToCassandra(CNRS.KEYSPACE, Annotation.tableName, Annotation.columns)

      whenDebug {
        val uniqLabels = dfAnnotation.select($"description").distinct().collect().map(_(0))
        debug(s"Total ${uniqLabels.length} annotation labels found in '$filename': ${uniqLabels.mkString(", ")}")
      }
    }
  }

  private def toAnnotation(source: Row): Option[Annotation] = {
     parseLocation(source.getAs[String]("location")) match {
       case Some((startLocation, endLocation)) =>
         val startTime = ts2UnixTS(source.getAs[String]("start_time"))
         val endTime = ts2UnixTS(source.getAs[String]("end_time"))
         val eventNum = source.getAs[Int]("event_num")
         val description = source.getAs[String]("description")
         val sens = source.getAs[Int]("sens")

         Some(Annotation(startTime, endTime, eventNum, description, sens, startLocation, endLocation ))

       case _ => None
     }
  }

}
