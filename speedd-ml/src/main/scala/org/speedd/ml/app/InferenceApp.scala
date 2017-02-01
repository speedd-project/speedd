package org.speedd.ml.app

import java.io.File
import auxlib.log.Logging
import auxlib.opt.OptionParser
import lomrf.logic.AtomSignature
import org.speedd.ml.ModuleVersion
import org.speedd.ml.util.logic._
import lomrf.util.time._
import org.speedd.ml.inference.Reasoner
import org.speedd.ml.inference._
import scala.util.{Success, Try}

object InferenceApp extends App with OptionParser with Logging {

  println(s"${ModuleVersion()}\nInference Application")

  // -------------------------------------------------------------------------------------------------------------------
  // --- Variables accessible from command-line
  // -------------------------------------------------------------------------------------------------------------------
  private var inputKBOpt: Option[File] = None
  private var sqlFunctionsFileOpt: Option[File] = None
  private var sqlAtomsFileOpt: Option[File] = None
  private var intervalOpt: Option[(Int, Int)] = None
  private var batchSizeOpt: Option[Int] = None
  private var simulationIdsOpt: Option[List[Int]] = None
  private var locationIdsOpt: Option[Iterable[String]] = None
  private var taskOpt: Option[String] = None

  private var queryPredicates = Set(AtomSignature("HoldsAt", 2))
  private var evidencePredicates = Set(
      AtomSignature("HappensAt", 2),
      AtomSignature("InitiatedAt", 2),
      AtomSignature("TerminatedAt", 2),
      AtomSignature("Next", 2)
  )

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

  opt("f2sql", "sql-function-mappings", "<string>", "Specify the sql function mappings file containing mappings of event functions to sql constraints.", {
    v: String =>
      val file = new File(v)

      sqlFunctionsFileOpt = {
        if (!file.isFile) fatal("The specified function mappings file does not exist.")
        else if (!file.canRead) fatal("Cannot read the specified sql function mappings file, please check the file permissions.")
        else Some(file)
      }
  })

  opt("a2sql", "sql-atom-mappings", "<string>", "Specify the sql atom mappings file containing mappings of atoms to sql constraints.", {
    v: String =>
      val file = new File(v)

      sqlAtomsFileOpt = {
        if (!file.isFile) fatal("The specified atom mappings file does not exist.")
        else if (!file.canRead) fatal("Cannot read the specified sql atom mappings file, please check the file permissions.")
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

  opt("sids", "simulation-ids", "Comma separated <sid>", "Specify the simulation id set used for training, e.g. 1,2,5", {
    v: String =>
      val sids = v.split(",")
      simulationIdsOpt = Option {
        Try(sids.map(_.toInt).toList) getOrElse fatal("Please specify a valid set of simulation ids. For example: 1,2,5")
      }
  })

  opt("lids", "location-ids", "Comma separated <lid>", "Specify the location id set used for training, e.g. 1324,2454,5128", {
    v: String => locationIdsOpt = Option(v.split(",").toIterable)
  })

  opt("t", "task", "<string>", "The name of the task to call (cnrs.collected, cnrs.simulation.highway or fz).", {
    v: String => taskOpt = Some(v.trim.toLowerCase)
  })

  opt("e", "evidence-predicates", "<string>", "Comma separated evidence atoms. Each atom must be defined using its "+
    "identity (i.e., Name/arity). For example the identity of predicate HappensAt(event, time) is 'HappensAt/2'. " +
    "'HappensAt/2, InitiatedAt/2, TerminatedAt/2 and Next/2' are the default evidence atoms.", {
    v: String =>
      evidencePredicates = parseSignatures(v) getOrElse {
        fatal(s"Failed to parse the atomic signatures of the specified evidence predicates '$v'. " +
          s"Please make sure that you gave the correct format.")
      }
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

  // File indicating the mappings of event functions or atoms to sql statements
  val sqlMappingsFile =
    if (sqlFunctionsFileOpt.isDefined) sqlFunctionsFileOpt.get
    else if (sqlAtomsFileOpt.isDefined) sqlAtomsFileOpt.get
    else fatal("Please specify sql mappings file")

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

  // Check if simulation ids exist, otherwise return an empty list
  val simulationIds = simulationIdsOpt.getOrElse(List.empty)

  // Check if location ids exist, otherwise return an empty list
  val locationIds = if (locationIdsOpt.isDefined) taskOpt match {
    case Some("cnrs.collected") => Some(Map("loc_id" -> locationIdsOpt.get))
    case Some("cnrs.simulation.highway") => Some(Map("section_id" -> locationIdsOpt.get))
  }
  else None

  import org.speedd.ml.util.data.DatabaseManager._

  // --- 1. Create the appropriate instance of inference engine
  val inferenceEngine: Reasoner = taskOpt.getOrElse(fatal("Please specify a task name")) match {
    case "cnrs.collected" => cnrs.collected.InferenceEngine(kbFile, sqlMappingsFile, evidencePredicates, queryPredicates)
    case "cnrs.simulation.highway" =>
      if (simulationIds.isEmpty) fatal("Please specify a set of simulation ids for this task!")
      cnrs.simulation.highway.InferenceEngine(kbFile, sqlMappingsFile, evidencePredicates, queryPredicates)
    case "fz" =>
      fz.InferenceEngine(kbFile, sqlMappingsFile, evidencePredicates, queryPredicates)
    case "cnrs.simulation.city" =>
      fatal(s"Task '${taskOpt.get}' is not implemented yet!")
    case _ => fatal(s"Unknown task '${taskOpt.get}'. Please specify a valid task name.")
  }

  // --- 2. Perform inference for all intervals
  val t = System.currentTimeMillis()
  inferenceEngine.inferFor(startTime, endTime, batchSize, locationIds, simulationIds)
  info(s"Inference for task ${taskOpt.get} completed in ${msecTimeToTextUntilNow(t)}")

  // --- 3. Close database connection
  closeConnection()
}
