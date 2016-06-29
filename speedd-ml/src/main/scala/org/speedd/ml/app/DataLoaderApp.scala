package org.speedd.ml.app

import org.speedd.ml.loaders.{DataLoader, cnrs}
import scala.util.Success
import lomrf.util.time._

/**
  * Command line interface for loading the CNRS real or simulated data into the database.
  */
object DataLoaderApp extends CLIDataLoaderApp {

  // -------------------------------------------------------------------------------------------------------------------
  // --- Configuration parameters
  // -------------------------------------------------------------------------------------------------------------------
  private var taskOpt: Option[String] = None

  // -------------------------------------------------------------------------------------------------------------------
  // --- Command line interface options
  // -------------------------------------------------------------------------------------------------------------------
  opt("t", "task", "Specify one of the following tasks: collected.input, collected.annotation, collected.location," +
    " simulation.input, simulation.annotation, simulation.location.", {
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

  // -------------------------------------------------------------------------------------------------------------------
  // --- Setup database connection pool
  // -------------------------------------------------------------------------------------------------------------------
  import org.speedd.ml.util.data.DatabaseManager._
  createSchema("cnrs")

  // --- 1. Check/prepare input files
  info(s"Checking/preparing input files in '$rootDir'")


  val inputFiles = filesFunc(rootDir.toFile, recursion) match {
    case Success(files) if files.nonEmpty => files
    case Success(files) => fatal("Didn't find any matching file")
    case _ => fatal("Please specify input CSV files.")
  }

  info(s"Parsing ${inputFiles.size} input files")

  // --- 2. Create the appropriate instance of data loader
  val loader: DataLoader = taskOpt.getOrElse(fatal("Please specify a task")) match {
    case "collected.input" => cnrs.collected.InputDataLoader
    case "collected.annotation" => cnrs.collected.AnnotationDataLoader
    case "collected.location" => cnrs.collected.LocationDataLoader
    case "simulation.input" => cnrs.simulation.InputDataLoader
    case "simulation.annotation" => cnrs.simulation.AnnotationDataLoader
    case "simulation.location" => cnrs.simulation.LocationDataLoader
    case name =>
      fatal(s"Unknown task '$name', please set one of the following tasks: (1) input, (2) annotation or (3) location.")
  }

  // --- 3. Execute data loading task
  val t = System.currentTimeMillis()
  loader.loadAll(inputFiles)
  info(s"Data loading completed in ${msecTimeToTextUntilNow(t)}")

  // --- 4. Close database connection
  closeConnection()
}

