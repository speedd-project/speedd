package org.speedd.ml.app

import java.io.File

import auxlib.log.Logging
import auxlib.opt.OptionParser
import lomrf.logic.AtomSignature
import lomrf.util.time._
import org.speedd.ml.ModuleVersion
import org.speedd.ml.learners.cnrs._
import org.speedd.ml.learners.Learner
import org.speedd.ml.util.logic._

import scala.util.{Success, Try}

object WeightLearningApp extends App with OptionParser with Logging {

  info(s"${ModuleVersion()}\nWeight Learning Application")

  // -------------------------------------------------------------------------------------------------------------------
  // --- Variables accessible from command-line
  // -------------------------------------------------------------------------------------------------------------------
  private var inputKBOpt: Option[File] = None
  private var outputKBOpt: Option[File] = None
  private var sqlFunctionsFileOpt: Option[File] = None
  private var sqlAtomsFileOpt: Option[File] = None
  private var intervalOpt: Option[(Int, Int)] = None
  private var excludeIntervalOpt: Option[(Int, Int)] = None
  private var batchSizeOpt: Option[Long] = None
  private var simulationIdsOpt: Option[List[Int]] = None
  private var locationIdsOpt: Option[Iterable[String]] = None
  private var taskOpt: Option[String] = None

  private var lambda: Double = 0.01
  private var eta: Double = 1.0

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

  opt("i", "interval", "<start time-point>,<end time-point>", "Specify the temporal interval for training, e.g. 10,100", {
    v: String =>
      val t = v.split(",")
      if(t.length != 2) fatal("Please specify a valid temporal interval, e.g. 10,100")
      else intervalOpt = Option {
        Try((t(0).toInt, t(1).toInt)) getOrElse fatal("Please specify a valid temporal interval. For example: 10,100")
      }
  })

  opt("exclude", "exclude-interval", "<start time-point>,<end time-point>", "Specify the temporal intervals to exclude from training, e.g. 20,40 ", {
    v: String =>
      val t = v.split(",")
      if(t.length != 2) fatal("Please specify a valid temporal interval, e.g. 20,40")
      else excludeIntervalOpt = Option {
        Try((t(0).toInt, t(1).toInt)) getOrElse fatal("Please specify a valid temporal interval. For example: 10,100")
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

  opt("t", "task", "<string>", "The name of the task to call (cnrs.collected, cnrs.simulation.city, cnrs.simulation.highway or fz).", {
    v: String => taskOpt = Some(v.trim.toLowerCase)
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

  doubleOpt("lambda", "lambda", "Regularization parameter (default is " + lambda + ").", {
    v: Double => if (v < 0) fatal("Regularization parameter must be any double above zero, but you gave: " + v) else lambda = v
  })

  doubleOpt("eta", "eta", "Learning rate (default is " + eta + ").", {
    v: Double => if (v < 0) fatal("Learning rate must be any double above zero, but you gave: " + v) else eta = v
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

  // File indicating the mappings of event functions to sql statements
  val sqlFunctionMappingsFile = sqlFunctionsFileOpt getOrElse fatal("Please specify sql function mappings file")

  // The temporal interval by which we will take the evidence that annotation data
  val (startTime, endTime) = intervalOpt getOrElse fatal("Please specify an interval")
  val intervalLength =
    if (excludeIntervalOpt.isDefined)
      endTime - startTime - (excludeIntervalOpt.get._2 - excludeIntervalOpt.get._1)
    else
      endTime - startTime

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

  // Check if simulation ids exist, otherwise return an empty list
  val simulationIds = simulationIdsOpt.getOrElse(List.empty)

  // Check if location ids exist, otherwise return an empty list
  val locationIds = if (locationIdsOpt.isDefined) taskOpt match {
    case Some("cnrs.collected") => Some(Map("loc_id" -> locationIdsOpt.get))
    case Some("cnrs.simulation.highway") => Some(Map("section_id" -> locationIdsOpt.get))
  }
  else None

  import org.speedd.ml.util.data.DatabaseManager._

  // --- 1. Create the appropriate instance of weight learner
  val weightLearner: Learner = taskOpt.getOrElse(fatal("Please specify a task name")) match {
    case "cnrs.collected" =>
      collected.WeightLearner(kbFile, outputFile, sqlFunctionMappingsFile,
        evidencePredicates, targetPredicates, nonEvidencePredicates, lambda, eta)
    case "cnrs.simulation.highway" =>
      if (simulationIds.isEmpty) fatal("Please specify a set of simulation ids for this task!")
      simulation.highway.WeightLearner(kbFile, outputFile, sqlFunctionMappingsFile,
        evidencePredicates, targetPredicates, nonEvidencePredicates, lambda, eta)
    case "cnrs.simulation.city" | "fz" =>
      fatal(s"Task '${taskOpt.get}' is not implemented yet!")
    case _ => fatal(s"Unknown task '${taskOpt.get}'. Please specify a valid task name.")
  }

  // --- 2. Perform training for all intervals and simulation ids (if any exists)
  val t = System.currentTimeMillis()
  weightLearner.trainFor(startTime, endTime, batchSize, excludeIntervalOpt, locationIds, simulationIds)
  info(s"Weight learning for task ${taskOpt.get} completed in ${msecTimeToTextUntilNow(t)}")

  // --- 3. Close database connection
  closeConnection()
}
