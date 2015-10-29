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
import org.speedd.ml.learners.{WeightEstimator, FzWeightsEstimator, CNRSWeightsEstimator}
import org.speedd.ml.util.logic.parseSignatures

import scala.util.{Success, Try}


object WeightLearnerCLI extends App with CommonOptions with Logging {

  println(s"${ModuleVersion()}\nData Loading Application")

  // -------------------------------------------------------------------------------------------------------------------
  // --- Variables accessible from command-line
  // -------------------------------------------------------------------------------------------------------------------
  private var inputKBOpt: Option[File] = None
  private var outputKBOpt: Option[File] = None
  private var intervalOpt: Option[(Long, Long)] = None
  private var batchSizeOpt: Option[Long] = None
  private var taskOpt: Option[String] = None

  private var targetPredicates = Set(AtomSignature("InitiatedAt", 2), AtomSignature("TerminatedAt", 2))
  private var evidencePredicates = Set(AtomSignature("HappensAt", 2))
  private var nonEvidencePredicates = Set(AtomSignature("HoldsAt", 2))

  // -------------------------------------------------------------------------------------------------------------------
  // --- Command-line options
  // -------------------------------------------------------------------------------------------------------------------
  opt("in", "input-kb", "<file>", "Specify the input knowledge base file.", {
    v: String =>
      val file = new File(v)

      inputKBOpt = {
        if (!file.isFile) fatal("The specified input knowledge base file does not exist.")
        else if (!file.canRead) fatal("Cannot read the specified input knowledge base file, please check the file permissions.")
        else Some(file)
      }
  })

  opt("out", "output-kb", "<file>", "Specify the output knowledge file. By default is the base name of the input KB file " +
    "appended with '.out.mln', for example, if the input file is 'foo.mln' the default output will be foo.out.mln " +
    "and will be located in the same directory.", {
    v: String => outputKBOpt = Some(new File(v))
  })

  opt("i", "interval", "<start time-point>,<end time-point>", "Specify the temporal interval for training, e.g. 10,100 ", {
    v: String =>
      val t = v.split(",")
      if(t.length != 2) fatal("Please specify a valid temporal interval, e.g. 10,100")
      else intervalOpt = Option {
        Try((t(0).toLong, t(1).toLong)) getOrElse fatal("Please specify a valid temporal interval. For example: 10,100")
      }
  })

  opt("bs", "batch-size", "<integer>", "Specify the batch size for learning, must be less or equal to the given " +
    "interval (see -i parameter)", {
    v: String =>
      batchSizeOpt = Try(v.toLong) match {
        case Success(bs) if bs > 0 => Some(bs)
        case _ => fatal("Please specify a positive integer number for batch size, e.g., 10")
      }

  })

  opt("t", "task", "<string>", "The name of the task to call (CNRS or Feedzai).", {
    v: String => taskOpt = Some(v.trim.toUpperCase)
  })

  opt("target", "target-predicates", "<string>", "Comma separated target atoms. Target atoms are atoms that appear in the" +
    "head of event definition rules. These rule will be parsed and simplified before weight learning. Each atom must be " +
    "defined using its identity (i.e., Name/arity). For example the identity of predicate InitiatedAt(fluent, time) " +
    "is 'InitiatedAt/2'. 'InitiatedAt/2' and 'TerminatedAt/2' are the default target atoms.", {
      v: String =>
        targetPredicates = parseSignatures(v) getOrElse {
          fatal(s"Failed to parse the atomic signatures of the specified target predicates '$v'. " +
            s"Please make sure that you gave the correct format.")
        }
    })

  opt("ne", "non-evidence-predicates", "<string>", "Comma separated non-evidence atoms. Each atom must be defined using its " +
    "identity (i.e., Name/arity). For example the identity of predicate HoldsAt(fluent, time) is 'HoldsAt/2'. " +
    "'HoldsAtAt/2' is the default non-evidence atom.", {
    v: String =>
      nonEvidencePredicates = parseSignatures(v) getOrElse {
        fatal(s"Failed to parse the atomic signatures of the specified non-evidence predicates '$v'. " +
          s"Please make sure that you gave the correct format.")
      }
  })

  opt("e", "evidence-predicates", "<string>", "Comma separated evidence atoms. Each atom must be defined using its "+
      "identity (i.e., Name/arity). For example the identity of predicate HappensAt(event, time) is 'HappensAt/2'. " +
      "'HappensAt/2' is the default evidence atom.", {
      v: String =>
        evidencePredicates = parseSignatures(v) getOrElse {
          fatal(s"Failed to parse the atomic signatures of the specified evidence predicates '$v'. " +
            s"Please make sure that you gave the correct format.")
        }
  })

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

  // File indicating the input MLN knowledge file
  val kbFile = inputKBOpt getOrElse fatal("Please specify an initial KB file")

  // File indicating the output MLN knowledge file
  val outputFile = outputKBOpt getOrElse {
    val name = kbFile.getName
    new File(kbFile.getPath + name.substring(0, name.lastIndexOf('.')) + "_trained.mln")
  }

  // The temporal interval by which we will take the evidence that annotation data
  val (startTime, endTime) = intervalOpt getOrElse fatal("Please specify an interval")
  val intervalLength = endTime - startTime

  // The batch size of the data that will given each time to the online learner
  val batchSize = batchSizeOpt match {
    case Some(bs) if bs <= Int.MaxValue =>
      // batch size must be bs <= intervalLength
      if (bs <= intervalLength) bs.toInt
      else fatal(s"Please specify a batch size that is less or equal to the length ($intervalLength) of the specified interval [$startTime, $endTime]")
    case Some(bs) if bs > Int.MaxValue =>
      fatal(s"Batch sizes greater that ${Int.MaxValue} are not supported, please give a smaller number.")
    case _ => fatal("Please specify a batch size")
  }



  // --- 2. Prepare Spark context
  val conf = new SparkConf()
    .setAppName(appName)
    .set("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
    .set("spark.cassandra.connection.host", cassandraConnectionHost)
    .set("spark.cassandra.connection.port", cassandraConnectionPort)
  //.setMaster(master)


  info(s"Spark configuration:\n${conf.toDebugString}")

  implicit val sc = new SparkContext(conf)
  info(s"Spark context initialised)")

  implicit val sqlContext = new SQLContext(sc)
  info(s"SparkSQL context initialised")

  val weightEstimator: WeightEstimator = taskOpt.getOrElse(fatal("Please specify a task name")) match {
    case "CNRS" => CNRSWeightsEstimator(kbFile,outputFile,evidencePredicates, targetPredicates)
    //case "FEEDZAI" => FzWeightsEstimator
    case _ => fatal("Please specify a task name")
  }



  sys.addShutdownHook {
    info("Shutting down Spark Context")
    sc.stop()
  }

  weightEstimator.trainFor(startTime, endTime, batchSize)
}
