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

package org.speedd.ml.util

import org.apache.spark.rdd.RDD
import org.apache.spark.sql.types.{StructType, LongType, StructField}
import org.apache.spark.sql.{Row, DataFrame}

object DataFrameOps {
  implicit class WrappedDF(val df: DataFrame) extends AnyVal {

    /**
     * Adds index numbers to the rows of the data frame
     *
     * @param colName the column name in which the index numbers will be stored (default is `uid`)
     * @param inFront specify if the location of the index column will be in front of the other columns, or in back
     *                (default is true)
     *
     * @return the indexed data frame
     */
    def zipWithIndex(colName: String = "index", inFront: Boolean = true, offset: Int = 0): DataFrame = {
      composeIndexedDataFrame(df.rdd.zipWithIndex(), colName, inFront, offset)
    }

    /**
     * Add a unique id to the rows of the data frame
     *
     * @param colName the column name in which the unique ids will be stored (default is `uid`)
     * @param inFront specify if the location of the unique id will be in front of the other columns, or in back
     *                (default is true)
     *
     * @return data frame with containing rows that are associated with a unique id
     */
    def zipWithUniqueId(colName: String = "uid", inFront: Boolean = true): DataFrame = {
      composeIndexedDataFrame(df.rdd.zipWithUniqueId(), colName, inFront, 0)
    }


    private def composeIndexedDataFrame(rdd: RDD[(Row, Long)], colName: String, inFront: Boolean, offset: Int): DataFrame ={
      val indexedRDD = rdd.map {
        case (row, idx) =>
          val indexSeq = Seq(idx + offset)
          val rowSeq = row.toSeq
          Row.fromSeq (if (inFront) indexSeq ++ rowSeq else rowSeq ++ indexSeq)
      }

      val oldColumns = df.schema.fields
      val indexColumn = StructField(colName, LongType, nullable = false)

      val columns = new Array[StructField](oldColumns.length + 1)

      if(inFront) {
        columns(0) = indexColumn
        System.arraycopy(oldColumns, 0, columns, 1, oldColumns.length)
      }
      else {
        System.arraycopy(oldColumns, 0, columns, 0, oldColumns.length)
        columns(columns.length - 1) = indexColumn
      }

      df.sqlContext.createDataFrame(indexedRDD, StructType(columns))
    }


  }

}
