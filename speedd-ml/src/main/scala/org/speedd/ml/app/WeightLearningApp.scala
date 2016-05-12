package org.speedd.ml.app

import java.io.File
import auxlib.log.Logging
import auxlib.opt.OptionParser
import lomrf.logic.AtomSignature
import org.speedd.ml.ModuleVersion
import org.speedd.ml.learners.cnrs.collected.CNRSWeightLearner
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
  private var intervalOpt: Option[(Int, Int)] = None
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

  opt("t", "task", "<string>", "The name of the task to call (CNRS or FZ).", {
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

  import org.speedd.ml.util.data.DatabaseManager._

  // --- 1. Create the appropriate instance of weight learner
  val weightLearner: Learner = taskOpt.getOrElse(fatal("Please specify a task name")) match {
    case "CNRS" => CNRSWeightLearner(kbFile, outputFile, evidencePredicates, targetPredicates)
    case _ => fatal("Please specify a task name")
  }

  // --- 2. Perform training for all intervals
  weightLearner.trainFor(startTime, endTime, batchSize)

  // --- 3. Close database connection
  closeConnection()
}
