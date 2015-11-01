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
      .options(Map("table" -> Annotation.tableName, "keyspace" -> KEYSPACE))
      .load()
  }
}