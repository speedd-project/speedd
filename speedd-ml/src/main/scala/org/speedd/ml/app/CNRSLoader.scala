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

package org.speedd.ml.app


import org.apache.spark.sql._
import org.apache.spark.{SparkConf, SparkContext}
import org.speedd.ml.model.CNRS
import scala.language.implicitConversions
import scala.util.Success
import org.speedd.ml.loaders.{FileLoader, cnrs}

object CNRSLoader extends CLIDataLoaderApp {

  // -------------------------------------------------------------------------------------------------------------------
  // --- Configuration parameters
  // -------------------------------------------------------------------------------------------------------------------

  private var taskOpt: Option[String] = None

  // -------------------------------------------------------------------------------------------------------------------
  // --- Command line interface options
  // -------------------------------------------------------------------------------------------------------------------
  opt("t", "task", "Specify the one of the following tasks: input, annotation, location.", {
     v: String => taskOpt = Some(v.trim.toLowerCase)
  })

  // -------------------------------------------------------------------------------------------------------------------
  // --- Application
  // -------------------------------------------------------------------------------------------------------------------

  if(args.isEmpty) {
    println(usage)
    sys.exit(1)
  }

  if(!parse(args)) fatal("Failed to parse the given arguments.")

  // --- 1. Check/prepare input files
  info(s"Checking/preparing input files in '$rootDir'")


  val inputFiles = filesFunc(rootDir.toFile, recursion) match {
    case Success(files) if files.nonEmpty => files
    case Success(files) => fatal("Didn't find any matching file")
    case _ => fatal("Please specify input CSV files.")
  }

  info(s"Will parse ${inputFiles.size} input files")

  // --- 2. Prepare Spark context
  val conf = new SparkConf()
    .setAppName(appName)
    .setMaster(master)
    .set("spark.cassandra.connection.host", cassandraConnectionHost)
    .set("spark.cassandra.connection.port", cassandraConnectionPort)
    //.set("spark.cassandra.input.split.size_in_mb", "16")
    //.set("spark.cassandra.input.split.size_in_mb", "67108864")

  info(s"Spark configuration:\n${conf.toDebugString}")

  implicit val sc = new SparkContext(conf)
  info(s"Spark context initialised)")

  implicit val sqlContext = new SQLContext(sc)
  info(s"SparkSQL context initialised")

  // --- 3. Check/prepare database schema
  info("Initializing schema (if not exist)")
  CNRS.initialize()

  // --- 4. Execute data loading task
  val loader: FileLoader = taskOpt.getOrElse(fatal("Please specify a task")) match {
    case "input" => cnrs.RawCSVDataLoader
    case "annotation" => cnrs.AnnotationDataLoader
    case "location" => cnrs.LocationDataLoader
    case v =>
      fatal(s"Unknown task name '$v', please set one of the following tasks: (1) input, (2) annotation or (3) location.")
  }

  loader.loadAll(inputFiles)

  sys.addShutdownHook{
    info("Shutting down Spark Context")
    sc.stop()
  }

}
