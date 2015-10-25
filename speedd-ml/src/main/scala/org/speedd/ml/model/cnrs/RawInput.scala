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

import org.apache.spark.sql.{DataFrame, SQLContext}
import org.speedd.ml.model.EntityFactory

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
object RawInput extends EntityFactory {

  override type SCHEMA = (Long, String, Int, Option[Double], Option[Double], Option[Double], Map[Int, Double], Map[Int, Double])

  override val tableName = "raw_input"

  override val keyspace: String = KEYSPACE

  override val columnNames = IndexedSeq("loc_id", "lane", "timestamp", "avg_speed", "occupancy", "vehicles", "hl", "hs")

  override def loadDF(implicit sqlContext: SQLContext): DataFrame = {
    import sqlContext.implicits._

    // discretize numeric values such as `occupancy`,`avg_speed`, etc.
    val mappedInputColumns = columnNames.map(colName => domain2udf.get(colName) match {
      case Some(f) => f($"$colName") as colName
      case None => $"$colName"
    })

    sqlContext.read
      .format("org.apache.spark.sql.cassandra")
      .options(Map("table" -> RawInput.tableName, "keyspace" -> KEYSPACE))
      .load()
      .select(mappedInputColumns: _*)
  }

}
