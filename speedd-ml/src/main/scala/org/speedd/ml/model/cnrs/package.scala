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

import org.apache.spark.SparkContext
import org.apache.spark.sql.{DataFrame, SQLContext}
import org.speedd.ml.util.data._

package object cnrs {
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

  /**
   * Mapping of column name to user defined function
   */
  val domain2udf = Map(
    "occupancy" -> mkSymbolicDiscretizerUDF(occLevels, "O"),
    "avg_speed" -> mkSymbolicDiscretizerUDF(speedLevels, "S"),
    "vehicles" -> mkSymbolicDiscretizerUDF(vehicleLevels, "V")
  )

  /**
   * Column names in the DB tables, that are aliases of MLN domains
   */
  val domainAliases = Map("start_loc" -> "loc_id", "end_loc" -> "loc_id")

  def loadFor(startTime: Long, endTime: Long)(implicit sc: SparkContext, sqlContext: SQLContext) = {
    import sqlContext.implicits._

    // make sure that we have all time-points of the specified temporal interval
    val timestampsDF = sc.parallelize(startTime to endTime).toDF("timestamp").cache()

    // ---
    // --- Load raw data for the specified temporal interval
    // ---
    val rawInputDF = RawInput.loadDF
      .where(s"timestamp >= $startTime AND timestamp <= $endTime")
      .cache()


    // ---
    // --- Load location data for the specified temporal interval
    // ---
    val locationDF = Location.loadDF.cache()

    // ---
    // --- Load annotation data for the specified interval
    // ---
    val annotatedLocations = Annotation.loadDF
      .where(s"start_ts <= $endTime AND end_ts >= $startTime")
      .withColumnRenamed("event_num", "eid")
      .cache()

    val annotationsDF = expandAnnotation(annotatedLocations, locationDF, timestampsDF).cache()

    timestampsDF.unpersist()

    (rawInputDF, locationDF, annotationsDF, annotatedLocations)
  }

  private def expandAnnotation(annotatedLocations: DataFrame, locationDF: DataFrame, timestampsDF: DataFrame)
                              (implicit sc: SparkContext, sqlContext: SQLContext): DataFrame = {
    import sqlContext.implicits._

    val annotatedLocationWIds = locationDF.select("loc_id", "dist", "lane").distinct()
      .join(annotatedLocations, $"dist" <= $"start_loc" and $"dist" >= $"end_loc")
      .select("loc_id", "start_ts", "end_ts", "description", "lane")
      .flatMap { r =>
        val loc_id = r.getLong(0)
        val description = r.getString(3)
        val lane = r.getString(4)
        (r.getInt(1) to r.getInt(2)).map(t => (t, loc_id, description, lane))
      }
      .toDF("ts", "loc_id", "description", "lane")

    // We take the left outer join between `timestampsDF` and `overlappingAnnotations`, in order to collect the
    // annotation data (including missing instances as nulls) for the specified temporal interval [startTime, endTime].
    val result = timestampsDF
      .join(annotatedLocationWIds, $"timestamp" === $"ts", "left_outer")
      .select("timestamp", "loc_id", "description", "lane")

    result
  }

}
