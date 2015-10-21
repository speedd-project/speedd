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

import lomrf.logic.AtomSignature
import org.apache.spark.sql.SQLContext
import org.apache.spark.{SparkContext, SparkConf}
import org.speedd.ml.ModuleVersion
import java.io.File
import auxlib.log.Logging
import com.datastax.spark.connector._
import org.speedd.ml.learners.CNRSWeightsEstimator


object WeightLearnerCLI extends App with CommonOptions with Logging {

  println(s"${ModuleVersion()}\nData Loading Application")

  // -------------------------------------------------------------------------------------------------------------------
  // --- Variables accessible from command-line
  // -------------------------------------------------------------------------------------------------------------------

  private var inputKBOpt: Option[File] = None
  private var outputKBOpt: Option[File] = None
  private var intervalOpt: Option[(Long, Long)] = None
  private var taskOpt: Option[String] = None

  // -------------------------------------------------------------------------------------------------------------------
  // --- Command-line options
  // -------------------------------------------------------------------------------------------------------------------
  opt("in", "input-kb", "<file>", "Specify the input knowledge base file.", {
    v: String =>
      val file = new File(v)

      inputKBOpt =
        if(!file.isFile) fatal("The specified input knowledge base file does not exist.")
        else if(!file.canRead) fatal("Cannot read the specified input knowledge base file, please check the file permissions.")
        else Some(file)
  })

  opt("out", "output-kb", "<file>",
  "Specify the output knowledge file. " +
    "By default is the base name of the input KB file appended with '.out.mln', " +
    "for example, if the input file is 'foo.mln' the default output will be foo.out.mln " +
    "and will be located in the same directory.", {
    v: String => outputKBOpt = Some(new File(v))
  })

  opt("i", "interval", "<start time-point>,<end time-point>", "Specify the temporal interval for training, e.g. 10,100 ", {
    v: String =>
      val t = v.split(",")
      if(t.length != 2) {
        fatal("Please specify a valid temporal interval, e.g. 10,100")
      } else try{
        intervalOpt = Some((t(0).toLong, t(1).toLong))
      } catch {
        case e: Exception =>
          fatal("Please specify a valid temporal interval, e.g. 10,100")
      }
  })

  opt("bs", "batch-size", "<integer>", "Specify the batch size for learning, must be less or equal to the given " +
    "interval (see -i parameter)", {
    v: String => try {
      v.toLong
    } catch {
      case e: Exception =>
        fatal("Please specify a valid batch size, e.g. 10")
    }
  })

  opt("t", "task", "<string>", "The name of the task to call (CNRS or Feedzai).", {
    v: String => taskOpt = Some(v.trim.toUpperCase)
  })

  /*opt("ne", "non-evidence", "<string>", "Comma separated non-evidence atoms. Each atom must be defined using its " +
    "identity (i.e. Name/arity). For example the identity of predicate HoldsAt(fluent, time) is HoldsAt/2", {
    v: String =>
  })*/

  flagOpt("v", "version", "Print version and exit.", sys.exit(0))

  flagOpt("h", "help", "Print usage options.", {
    println(usage)
    sys.exit(0)
  })




  // -------------------------------------------------------------------------------------------------------------------
  // --- Application
  // -------------------------------------------------------------------------------------------------------------------

  if(args.isEmpty){
    println(usage)
    sys.exit(1)
  }

  if(!parse(args)) fatal("Failed to parse the given arguments.")


  val kbFile = inputKBOpt.getOrElse(fatal("Please specify an initial KB file"))
  val outputFile = outputKBOpt getOrElse {
    val name = kbFile.getName
    new File(kbFile.getPath + name.substring(0, name.lastIndexOf('.')) + "_trained.mln")
  }

  /*val (startTime, endTime) = intervalOpt.getOrElse(fatal("Please specify an interval"))

  taskOpt.getOrElse(fatal("Please specify a translator name")) match {
    case "CNRS" => learnCNRS()

    case "FEEDZAI" => //todo

    case _ => fatal("Please specify a valid translator name")
  }*/


  // --- 2. Prepare Spark context

  val conf = new SparkConf()
    .setAppName(appName)
    .setMaster(master)
    .set("spark.cassandra.connection.host", cassandraConnectionHost)
    .set("spark.cassandra.input.split.size_in_mb", "67108864")
    .set("spark.executor.memory", "1g")
    .set("spark.driver.memory", "2g")


  info(s"Spark configuration:\n${conf.toDebugString}")

  implicit val sc = new SparkContext(conf)
  info(s"Spark context initialised)")

  implicit val sqlContext = new SQLContext(sc)
  info(s"SparkSQL context initialised")

  /*val cc = new CassandraSQLContext(sc)
  cc.setKeyspace("cnrs")*/


  // -------------------------------------------------------------------------------------------------------------------
  // --- demo
  // -------------------------------------------------------------------------------------------------------------------
  val (startTime, endTime) = (1397041487, 1397043127)
  val inputPredicates = Set(AtomSignature("HappensAt", 2))
  val targetPredicates = Set(AtomSignature("InitiatedAt", 2), AtomSignature("TerminatedAt", 2))

  CNRSWeightsEstimator.learn(startTime, endTime, kbFile, outputFile, inputPredicates, targetPredicates)
  // -------------------------------------------------------------------------------------------------------------------

  sys.addShutdownHook{
    info("Shutting down Spark Context")
    sc.stop()
  }

}
