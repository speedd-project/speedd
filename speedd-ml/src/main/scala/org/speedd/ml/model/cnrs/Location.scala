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

package org.speedd.ml.model.cnrs

import com.datastax.spark.connector.SomeColumns
import org.apache.spark.sql.{DataFrame, SQLContext}

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
      .options(Map("table" -> Location.tableName, "keyspace" -> KEYSPACE))
      .load()
  }
}
