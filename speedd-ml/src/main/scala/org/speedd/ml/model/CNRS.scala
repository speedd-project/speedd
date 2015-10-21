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
import org.apache.spark.SparkContext
import org.apache.spark.sql.ColumnName
import language.implicitConversions

/**
 * This object contains databases entities (a corresponding case class for each table) and a schema initialization
 * function.
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
   * Initializes the database schema (DDL).
   *
   * @param sc  implicit instance of the spark context
   */
  def initialize()(implicit sc: SparkContext): Unit ={
    val connection = CassandraConnector(sc.getConf)

    // DDL operations
    connection.withSessionDo { s =>
      s.execute("CREATE KEYSPACE IF NOT EXISTS cnrs WITH REPLICATION = {'class': 'SimpleStrategy', 'replication_factor': 1 }")
      s.execute(
        s"""
          |CREATE TABLE IF NOT EXISTS ${RawInput.fullName} (
          |loc_id bigint,
          |lane varchar,
          |timestamp int,
          |occupancy decimal,
          |vehicles decimal,
          |avg_speed decimal,
          |hs map<int,decimal>,
          |hl map<int,decimal>,
          |PRIMARY KEY ((loc_id, lane), timestamp)
          |)
          |WITH compression = { 'sstable_compression' : 'SnappyCompressor' };
          | """.stripMargin)


      s.execute(s"CREATE INDEX IF NOT EXISTS ON ${RawInput.fullName} (timestamp);")

      s.execute(
        s"""
          |CREATE TABLE IF NOT EXISTS ${Location.fullName} (
          |loc_id bigint,
          |lane varchar,
          |prev_lane varchar,
          |coord_x decimal,
          |coord_y decimal,
          |dist int,
          |num int,
          |PRIMARY KEY (loc_id, lane)
          |)
          |WITH compression = { 'sstable_compression' : 'SnappyCompressor' };
        """.stripMargin)

      s.execute(
        s"""
          |CREATE TABLE IF NOT EXISTS ${Annotation.fullName} (
          |start_ts int,
          |end_ts int,
          |event_num int,
          |description varchar,
          |end_loc int,
          |sens int,
          |start_loc int,
          |PRIMARY KEY ((start_ts), end_ts, event_num)
          |)
          |WITH compression = { 'sstable_compression' : 'SnappyCompressor' };
        """.stripMargin)

      s.execute(s"CREATE INDEX IF NOT EXISTS ON ${Annotation.fullName} (description);")
    }
  }

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

    val fullName = KEYSPACE+"."+tableName

    val columnNames = Seq("start_ts", "end_ts", "event_num", "description", "sens", "start_loc", "end_loc")

    val columns = SomeColumns("start_ts", "end_ts", "event_num", "description", "sens", "start_loc", "end_loc")
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

    val fullName = KEYSPACE+"."+tableName

    val columnNames = Seq("loc_id", "lane", "coord_x", "coord_y", "num", "prev_lane", "dist")

    val columns = SomeColumns("loc_id", "lane", "coord_x", "coord_y",  "num", "prev_lane", "dist")
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

    type SCHEMA = (Long, String, Int, Option[Double], Option[Double], Option[Double], Map[Int, Double], Map[Int, Double])

    val tableName = "raw_input"

    val fullName = KEYSPACE+"."+tableName

    val columnNames = Seq("loc_id", "lane", "timestamp", "avg_speed",  "occupancy", "vehicles", "hl", "hs")

    val columns = SomeColumns("loc_id", "lane", "timestamp", "avg_speed",  "occupancy", "vehicles", "hl", "hs")
  }

}
