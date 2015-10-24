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

package org.speedd.ml.model

import com.datastax.spark.connector.SomeColumns
import com.datastax.spark.connector.cql.CassandraConnector
import com.datastax.spark.connector.rdd.reader.RowReaderFactory
import org.apache.spark.SparkContext
import org.apache.spark.sql.{DataFrame, SQLContext, ColumnName}
import org.speedd.ml.util.data._
import language.implicitConversions
import scala.reflect.ClassTag

/**
 * This object contains databases entities (a corresponding case class for each table), as well as some use case
 * specific constants (e.g., user defined thresholds of average speed levels).
 */
object CNRS {

  /**
   * The Cassandra keyspace to use
   */
  val KEYSPACE = "cnrs"

  /**
   * PR0 to PR11 relative distances in meters from PR0.
   */
  val prDistances = Array(0, 962, 1996, 2971, 3961, 4961, 5948, 6960, 7971, 8961, 9966, 11035)

  /**
   * User defined occupancy levels
   */
  val occLevels = Array(0.0, 25.0, 50.0, 75.0)

  /**
   * User defined average speed levels
   */
  val speedLevels = Array(0.0, 50.0, 90.0, 100.0, 130.0)

  /**
   * User defined vehicle numbers
   */
  val vehicleLevels = Array(0.0, 4.0, 8.0, 16.0, 32.0)


  val domain2udf = Map(
    "occupancy" -> mkSymbolicDiscretizerUDF(CNRS.occLevels, "O"),
    "avg_speed" -> mkSymbolicDiscretizerUDF(CNRS.speedLevels, "S"),
    "vehicles" -> mkSymbolicDiscretizerUDF(CNRS.vehicleLevels, "V")
  )

  /**
   * Entity `Annotation`
   *
   * @param startTs start timestamp
   * @param endTs end timestamp
   * @param eventNum event number
   * @param description the target label
   * @param sens sensor type
   */
  case class Annotation(startTs: Int,
                        endTs: Int,
                        eventNum: Int,
                        description: String,
                        sens: Int,
                        startLoc: Int,
                        endLoc: Int)

  /**
   * Companion object of entity Annotation, in order to hold some utility constants
   */
  object Annotation {

    type SCHEMA = (Int, Int, Int, String, Int, Int, Int)

    val tableName = "annotation"

    val fullName = KEYSPACE + "." + tableName

    val columnNames = Seq("start_ts", "end_ts", "event_num", "description", "sens", "start_loc", "end_loc")

    val columns = SomeColumns("start_ts", "end_ts", "event_num", "description", "sens", "start_loc", "end_loc")


    def loadDF(implicit sqlContext: SQLContext): DataFrame = {
      sqlContext.read
        .format("org.apache.spark.sql.cassandra")
        .options(Map("table" -> Annotation.tableName, "keyspace" -> CNRS.KEYSPACE))
        .load()
    }
  }

  /**
   * Entity `Location`
   *
   * @param locId location id
   * @param lane lane type
   * @param prevLane previous lane type
   * @param coordX  x coordinate
   * @param coordY  y coordinate
   * @param num location number
   */
  case class Location(locId: Long,
                      lane: String,
                      prevLane: Option[String],
                      coordX: Double,
                      coordY: Double,
                      num: Int,
                      dist: Int)

  /**
   * Companion object of entity Location, in order to hold some utility constants
   */
  object Location {

    type SCHEMA = (Long, String, Option[String], Double, Double, Int, Int)

    val tableName = "location"

    val fullName = KEYSPACE + "." + tableName

    val columnNames = Seq("loc_id", "lane", "coord_x", "coord_y", "num", "prev_lane", "dist")

    val columns = SomeColumns("loc_id", "lane", "coord_x", "coord_y", "num", "prev_lane", "dist")

    def loadDF(implicit sqlContext: SQLContext): DataFrame = {
      sqlContext.read
        .format("org.apache.spark.sql.cassandra")
        .options(Map("table" -> Location.tableName, "keyspace" -> CNRS.KEYSPACE))
        .load()
    }
  }

  /**
   * Entity `RawInput`
   *
   * @param locId location id
   * @param lane lane type
   * @param timestamp timestamp
   * @param occupancy occupancy number
   * @param vehicles number of vehicles
   * @param avgSpeed average speed
   * @param hs speeds histogram
   * @param hl lengths histogram
   */
  case class RawInput(locId: Long,
                      lane: String,
                      timestamp: Int,
                      occupancy: Option[Double],
                      vehicles: Option[Double],
                      avgSpeed: Option[Double],
                      hs: Map[Int, Double],
                      hl: Map[Int, Double])

  /**
   * Companion object of entity RawInput, in order to hold some utility constants
   */
  object RawInput {

    import com.datastax.spark.connector._

    type SCHEMA = (Long, String, Int, Option[Double], Option[Double], Option[Double], Map[Int, Double], Map[Int, Double])

    val tableName = "raw_input"

    val fullName = KEYSPACE + "." + tableName

    val columnNames = Seq("loc_id", "lane", "timestamp", "avg_speed", "occupancy", "vehicles", "hl", "hs")

    val columns = SomeColumns("loc_id", "lane", "timestamp", "avg_speed", "occupancy", "vehicles", "hl", "hs")

    def loadDF(implicit sqlContext: SQLContext): DataFrame = {
      import sqlContext.implicits._

      // discretize numeric values such as `occupancy`,`avg_speed`, etc.
      val mappedInputColumns = columnNames.map(colName => domain2udf.get(colName) match {
        case Some(f) => f($"$colName") as colName
        case None => $"$colName"
      })

      sqlContext.read
        .format("org.apache.spark.sql.cassandra")
        .options(Map("table" -> RawInput.tableName, "keyspace" -> CNRS.KEYSPACE))
        .load()
        .select(mappedInputColumns: _*)
    }

  }

}
