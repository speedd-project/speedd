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

import java.util.Arrays._

import com.datastax.spark.connector.{ColumnName, ColumnRef}
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.{DataFrame, UserDefinedFunction}
import org.apache.spark.sql.functions._
import scala.collection.breakOut
import scala.language.implicitConversions

package object data {

  /**
   * Implicitly convert a sequence of string constants to a sequence of ColumnRef instances
   *
   * @param names collection of string constants
   *
   * @return the resulting sequence of ColumnRef instances
   */
  implicit def string2ColumnNames(names: Seq[String]): Seq[ColumnRef] = {
    names.map(name => new ColumnName(name))
  }

  /**
   * This function gives the distinct collection of symbols per column from a specified collection of Data Frames.
   * The columns that exist in more than one data frames, will be merged. Please note that all column values are
   * converted to Strings.
   *
   *
   * @param source the data frames to
   * @param columnAliases mapping of column aliases, i.e., columns with different names that we would like to be treated
   *                      as columns with the same name.
   *
   * @return a Map of column names to an RDD of their distinct symbols
   */
  def symbolsPerColumn(source: DataFrame*)(implicit columnAliases: Map[String, String] = Map.empty): Map[String, RDD[String]] = {

    def symbolsPerDFColumn(df: DataFrame): Map[String, DataFrame] = {
      df.columns.map { colName =>
        columnAliases.getOrElse(colName, colName) -> df.select(colName).filter(df(colName).isNotNull).distinct()
      }(breakOut)
    }

    def mergeColumns(m1: Map[String, DataFrame], m2: Map[String, DataFrame]): Map[String, DataFrame] = m1 ++ m2.map {
      case (k, v) => m1.get(k) match {
        case Some(d) => k -> d.unionAll(v)
        case None => k -> v
      }
    }

    source.map(symbolsPerDFColumn)
      .aggregate(Map.empty[String, DataFrame])(mergeColumns, mergeColumns)
      .mapValues(_.distinct().map(_.get(0).toString))
  }

  /**
   *
   * @param thresholds input array of user-defined threshold values
   * @param prefix a string to use as a prefix symbol
   *
   * @return the UDF that discretizes
   */
  def mkSymbolicDiscretizerUDF(thresholds: Array[Double], prefix: String): UserDefinedFunction = {
    val sortedThresholds = thresholds.sorted
    udf {
      (x: Double) =>
        val pos = binarySearch(sortedThresholds, x)
        val bin = if (pos < 0) -pos - 2 else pos
        prefix + bin
    }
  }

}
