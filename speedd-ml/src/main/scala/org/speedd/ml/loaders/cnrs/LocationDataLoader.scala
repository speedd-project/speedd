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
import org.speedd.ml.model.CNRS.Location
import org.speedd.ml.util.data.CSV._
import scala.language.implicitConversions
import com.datastax.spark.connector._

/**
 * Loads location data from CSV files. The data is collected and provided by CNRS.
 *
 */
object LocationDataLoader extends DataLoader with Logging {

  override def loadAll(inputFiles: Seq[File])(implicit sc: SparkContext, sqlContext: SQLContext): Unit = {
    import sqlContext.implicits._

    info("Loading location data")

    val csvSchema = parseSchema{
      "loc, lane, ?prev_lane, coordX:double, coordY:double, num:int, dist:int"
    }


    for {
      file <- inputFiles
      if file.isFile && file.canRead
      filename = file.getName} {

      info(s"Parsing file '$filename'")

      val locations = sqlContext.read
        .format(CSV_FORMAT)
        .options(CSV_OPTIONS)
        .schema(csvSchema)
        .load(file.getPath)
        .map(toLocation)
        .cache()

      info(s"Persisting the filtered contents from file '$filename'")

      locations.saveToCassandra(CNRS.KEYSPACE, Location.tableName, Location.columns)
      val nLocations = locations.count().toInt

      info(s"Saved location data from '$filename'. Total $nLocations locations persisted.")
    }
  }

  private def toLocation(source: Row): Location = {

    val prevLaneOpt = source.getAs[String]("prev_lane") match {
      case x if x != null && x.nonEmpty => Some(x)
      case _ => None
    }

    Location(
      java.lang.Long.valueOf(source.getAs[String]("loc"), 16),
      source.getAs[String]("lane"),
      prevLaneOpt,
      source.getAs[Double]("coordX"),
      source.getAs[Double]("coordY"),
      source.getAs[Int]("num"),
      source.getAs[Int]("dist")
    )
  }
}
