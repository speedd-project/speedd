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
import org.speedd.ml.model.cnrs._
import org.speedd.ml.util.data.CSV._
import java.sql.Timestamp
import scala.language.implicitConversions
import com.datastax.spark.connector._

import scala.collection.breakOut

/**
 * Loads and converts raw aggregated input sensor data from CSV files. The data is collected and provided by CNRS.
 *
 * <p>
 *  Each row in the input CSV file has the following format:
 * <ul>
 *   <li>date: a string in the form of YYYY-MM-DD</li>
 *   <li>time: a string in the form of hh:mm:ss</li>
 *   <li>loc: a string containing the unique hexadecimal number of location</li>
 *   <li>lane: string, indicates the lane type (e.g., onramp, slow, etc.)</li>
 *   <li>occupancy: (optional) a double precision number, indicates the lane occupancy</li>
 *   <li>vehicles: (optional) a double precision number, indicates</li>
 *   <li>med_speed: (optional) this column is ignored</li>
 *   <li>avg_speed: (optional) average speed </li>
 *   <li>columns 8 to 27: histogram of speeds (i.e., array of double precision numbers)</li>
 *   <li>columns 28 to 127: histogram of lengths (i.e., array of double precision numbers)</li>
 * </ul>
 * </p>
 *
 * <p>
 *  All input data is collected, transformed and stored as Cassandra entries. Please note that the following
 *  transformations are performed to each input CSV row:
 *  <ul>
 *    <li>Column `loc` is converted to 64 bit integer number (Long) and stored as `locid` in the database table (raw_input).</li>
 *
 *    <li>Columns `date` and `time` are merged and transformed into standard UNIX time-stamp format and the resulting
 *    column is stored as `timestamp` in the database table (raw_input).</li>
 *
 *    <li>The (possibly transformed) columns `loc_id`, `lane` and `timestamp` form the composite primary key for each
 *    data entry in the database table (raw_input).</li>
 *
 *    <li> When the values in columns `occupancy`, `vehicles` or `avg_speed` are equal or below to zero, are ignored and
 *    stored as null values in the database. </li>
 *
 *    <li>Columns 8 to 27 are stored as sparse histogram of speeds.</li>
 *
 *    <li>Columns 28 to 127 are stored as sparse histogram of lengths. </li>
 *  </ul>
 * </p>
 */
object RawCSVDataLoader extends DataLoader with Logging {

  // Schema definition for the speed histogram columns in the input CSV files
  private val hs = (0 to 200 by 10)
    .sliding(2)
    .map(e => s"s[${e.head}-${e.last}]:double")
    .mkString(",")

  // Schema definition for the lengths histogram columns in the input CSV files
  private val hl = (0.0f to 50.0f by 0.5f)
    .sliding(2)
    .map(e => s"l[${e.head}-${e.last}]:double")
    .mkString(",")

  // Schema definition for input CSV files (i.e., column types)
  private val inputCSVSchema = parseSchema{
    s"date, time, loc, lane, ?occupancy:double, ?vehicles:double, ?med_speed, ?avg_speed:double, $hs, $hl"
  }

  override def loadAll(inputFiles: Seq[File])(implicit sc: SparkContext, sqlContext: SQLContext): Unit = {
    import sqlContext.implicits._

    info("loading aggregated data from csv files")

    for {
      file <- inputFiles
      if file.isFile && file.canRead
      filename = file.getName} {

      info(s"Parsing file '$filename'")

      val startTime = System.currentTimeMillis()

      // Load input CSV and convert to an RDD of RawInput instances
      val rawInput = sqlContext.read
        .format(CSV_FORMAT)
        .options(CSV_OPTIONS)
        .schema(inputCSVSchema)
        .load(file.getPath)
        .flatMap(asRawInput)

      // Save RawInput to Cassandra database
      rawInput.saveToCassandra(KEYSPACE, RawInput.tableName, RawInput.columns)

      val endTime = System.currentTimeMillis()

      info(s"Persisted input data from '$filename'. Total transformation and storage time: ${endTime  - startTime} milliseconds.")
    }

  }


  /**
   * Converts Row instances (as they are loaded from input CSV file) to RawInput instances, in order to store them
   * in Cassandra database.
   *
   * @param source the source Row instance
   * @return the corresponding RawInput instance
   */
  private def asRawInput(source: Row): Option[RawInput] ={

    def mkSparseHistogram(range: Range): Map[Int, Double] ={
      (for {
        idx <- range
        value = source.getDouble(idx)
        if value > 0.0
      } yield idx -> value)(breakOut)
    }

    implicit def doubleToOpt(number: Double): Option[Double] = {
      if(number <= 0.0) None
      else Some(number)
    }

    val lane = source.getAs[String]("lane").trim

    if(lane.isEmpty) None
    else {
      // Please note that the arguments `occupancy`, `vehicles` and `avgSpeed` are implicitly converted to Option[Double]
      // instances. When their values are equal or below zero are considered as `None`, otherwise are instantiated as
      // Some(value).
      Some(
        RawInput(
          // Column `loc_id`:
          java.lang.Long.valueOf(source.getAs[String]("loc"), 16),

          // Column `lane`:
          lane,

          // Column `timestamp`:
          (Timestamp.valueOf(source.getAs[String]("date") + " " + source.getAs[String]("time")).getTime / 1000).toInt,

          // Column `occupancy`:
          source.getAs[Double]("occupancy"),

          // Column `vehicles`:
          source.getAs[Double]("vehicles"),

          // Column `vehicles`:
          source.getAs[Double]("avg_speed"),

          // Column `hs`:
          // Represent the histogram from columns 8 to 27, using a Map[position -> value] (sparse representation)
          // That sparse representation is the entry of the 7th column (array index=6)
          mkSparseHistogram(8 to 27),

          // Column `hl`:
          // Represent the histogram from columns 28 to 127, using a Map[position -> value] (sparse representation)
          // That sparse representation is the entry of the 8th column (array index=7)
          mkSparseHistogram(28 to 127)
        ))
    }
  }

}
