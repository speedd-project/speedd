package org.speedd.ml.app

import java.io.File
import auxlib.log.Logging
import auxlib.opt.OptionParser
import lomrf.logic.AtomSignature
import org.speedd.ml.ModuleVersion
import org.speedd.ml.inference.CNRSInferenceEngine
import org.speedd.ml.util.logic._
import lomrf.util.time._
import scala.util.{Success, Try}

object InferenceApp extends App with OptionParser with Logging {

  println(s"${ModuleVersion()}\nInference Application")

  // -------------------------------------------------------------------------------------------------------------------
  // --- Variables accessible from command-line
  // -------------------------------------------------------------------------------------------------------------------
  private var inputKBOpt: Option[File] = None
  private var atomMappingsOpt: Option[File] = None
  private var intervalOpt: Option[(Int, Int)] = None
  private var batchSizeOpt: Option[Int] = None
  private var taskOpt: Option[String] = None

  private var queryPredicates = Set(AtomSignature("HoldsAt", 2))

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

  opt("i", "interval", "<start time-point>,<end time-point>", "Specify the temporal interval for training, e.g. 10,100 ", {
    v: String =>
      val t = v.split(",")
      if(t.length != 2) fatal("Please specify a valid temporal interval, e.g. 10,100")
      else intervalOpt = Option {
        Try((t(0).toInt, t(1).toInt)) getOrElse fatal("Please specify a valid temporal interval. For example: 10,100")
      }
  })

  opt("bs", "batch-size", "<integer>", "Specify the batch size for learning, must be less or equal to the given " +
    "interval (see -i parameter)", {
    v: String =>
      batchSizeOpt = Try(v.toInt) match {
        case Success(bs) if bs > 0 => Some(bs)
        case _ => fatal("Please specify a positive integer number for batch size, e.g., 10")
      }

  })

  opt("t", "task", "<string>", "The name of the task to call (CNRS or FZ).", {
    v: String => taskOpt = Some(v.trim.toUpperCase)
  })

  opt("q", "query-predicates", "<string>", "Comma separated query atoms. Each atom must be defined using its " +
    "identity (i.e., Name/arity). For example the identity of predicate HoldsAt(fluent, time) is 'HoldsAt/2'. " +
    "'HoldsAtAt/2' is the default query atom.", {
    v: String =>
      queryPredicates = parseSignatures(v) getOrElse {
        fatal(s"Failed to parse the atomic signatures of the specified non-evidence predicates '$v'. " +
          s"Please make sure that you gave the correct format.")
      }
  })

  opt("am", "atom-mappings", "<string>", "Specify the atom mappings file containing mappings of predicates to sql constraints.", {
    v: String =>
      val file = new File(v)

      atomMappingsOpt = {
        if (!file.isFile) fatal("The specified atom mappings file does not exist.")
        else if (!file.canRead) fatal("Cannot read the specified atom mappings file, please check the file permissions.")
        else Some(file)
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
  val kbFile = inputKBOpt getOrElse fatal("Please specify an input KB file")

  // File indicating the input MLN knowledge file
  val atomMappingsFile = atomMappingsOpt getOrElse fatal("Please specify an atom mappings file")

  // The temporal interval by which we will take the evidence and annotation data for inference
  val (startTime, endTime) = intervalOpt getOrElse fatal("Please specify an interval")
  val intervalLength = endTime - startTime

  // The batch size of the data that will given each time for inference
  val batchSize = batchSizeOpt match {
    case Some(bs) if bs <= Int.MaxValue =>
      // batch size must be bs <= intervalLength
      if (bs <= intervalLength) bs.toInt
      else fatal(s"Please specify a batch size that is less or equal to the length ($intervalLength) of the specified interval [$startTime, $endTime]")
    case Some(bs) if bs > Int.MaxValue =>
      fatal(s"Batch sizes greater that ${Int.MaxValue} are not supported, please give a smaller number.")
    case _ => fatal("Please specify a batch size")
  }

  import org.speedd.ml.util.data.DatabaseManager._

  // --- 1. Create the appropriate instance of inference engine
  val inferenceEngine: CNRSInferenceEngine = taskOpt.getOrElse(fatal("Please specify a task name")) match {
    case "CNRS" => CNRSInferenceEngine(kbFile, atomMappingsFile, queryPredicates)
    case _ => fatal("Please specify a task name")
  }

  // --- 2. Perform inference for all intervals
  val t = System.currentTimeMillis()
  inferenceEngine.inferFor(startTime, endTime, batchSize)
  info(s"Inference for task ${taskOpt.get} completed in ${msecTimeToTextUntilNow(t)}")

  // --- 3. Close database connection
  closeConnection()
}
