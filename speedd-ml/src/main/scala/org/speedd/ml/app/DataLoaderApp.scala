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
  opt("t", "task", "Specify one of the following tasks: cnrs.collected.input, cnrs.collected.annotation," +
    " cnrs.collected.location, cnrs.simulation.city.input, cnrs.simulation.city.annotation, cnrs.simulation.city.location," +
    " cnrs.simulation.highway.input, cnrs.simulation.highway.annotation, cnrs.simulation.highway.location.", {
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
    case "cnrs.collected.input" => cnrs.collected.InputDataLoader
    case "cnrs.collected.annotation" => cnrs.collected.AnnotationDataLoader
    case "cnrs.collected.location" => cnrs.collected.LocationDataLoader
    case "cnrs.simulation.city.input" => cnrs.simulation.city.InputDataLoader
    case "cnrs.simulation.city.annotation" => cnrs.simulation.city.AnnotationDataLoader
    case "cnrs.simulation.city.location" => cnrs.simulation.city.LocationDataLoader
    case "cnrs.simulation.highway.input" => cnrs.simulation.highway.InputDataLoader
    case "cnrs.simulation.highway.annotation" => cnrs.simulation.highway.AnnotationDataLoader
    case "cnrs.simulation.highway.location" => cnrs.simulation.highway.LocationDataLoader
    case name =>
      fatal(s"Unknown task '$name', please set one of the following tasks:" +
        s"\n(1) cnrs.collected.input\n(2) cnrs.collected.annotation\n(3) cnrs.collected.location" +
        s"\n(4) cnrs.simulation.city.input\n(5) cnrs.simulation.city.annotation\n(6) cnrs.simulation.city.location" +
        s"\n(7) cnrs.simulation.highway.input\n(8) cnrs.simulation.highway.annotation\n(9) cnrs.simulation.highway.location")
  }

  // --- 3. Execute data loading task
  val t = System.currentTimeMillis()
  loader.loadAll(inputFiles)
  info(s"Data loading completed in ${msecTimeToTextUntilNow(t)}")

  // --- 4. Close database connection
  closeConnection()
}

