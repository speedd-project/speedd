package org.speedd.ml.app

import auxlib.log.Logging
import auxlib.opt.OptionParser
import org.speedd.ml.ModuleVersion
import org.speedd.ml.model.cnrs.simulation.{AnnotationData, InputData, LocationData}
import org.speedd.ml.util.data.Plotter._
import slick.driver.PostgresDriver.api._
import scala.util.Try

object SimulatedCNRSCityDataPlotApp extends App with OptionParser with Logging {

  import org.speedd.ml.util.data.DatabaseManager._

  println(s"${ModuleVersion()}\nSimulated CNRS 'City' Data Plot Application")

  // -------------------------------------------------------------------------------------------------------------------
  // --- Configuration parameters
  // -------------------------------------------------------------------------------------------------------------------
  private var dataOpt: Option[Seq[String]] = None
  private var simulationIdOpt: Option[Int] = None
  private var trafficLevelOpt: Option[String] = None
  private var sectionIdOpt: Option[Int] = None
  private var vehicleTypeOpt: Option[String] = None
  private var pdfOpt: Option[String] = None
  private var pngOpt: Option[String] = None

  val sectionIds = blockingExec {
    AnnotationData.join(LocationData)
      .on(_.sectionId === _.sectionId).map(_._1.sectionId).distinct.result
  }

  // -------------------------------------------------------------------------------------------------------------------
  // --- Command line interface options
  // -------------------------------------------------------------------------------------------------------------------

  intOpt("sid", "simulation-id", "<integer>", "Simulation id data to be plotted (1 to 10).", {
    v: Int =>
      if (v < 1 || v > 10) fatal("Please specify a valid simulation id, e.g. 1 to 10.")
      simulationIdOpt = Some(v)
  })

  opt("level", "traffic-level", "<string>", "Level of traffic (low or high).", {
    v: String =>
      if (v.toLowerCase != "high" && v.toLowerCase != "low") fatal("Please specify a valid level of traffic e.g. low or high.")
      trafficLevelOpt = Some(v.toLowerCase.capitalize)
  })

  intOpt("lid", "location-id", "<integer>", s"The location id data to plot. Location having annotation are $sectionIds", {
    v: Int =>
      if (!sectionIds.contains(v)) fatal(s"Please choose one of the following locations ${sectionIds.mkString(", ")}.")
      sectionIdOpt = Some(v)
  })

  opt("type", "vehicle-type", "<string>", "Plot data for specific vehicles types (all, car or trunk).", {
    v: String =>
      if(v.toLowerCase != "all" && v.toLowerCase != "car" && v.toLowerCase != "trunk") fatal("Please specify a vehicle type e.g. all, car or trunk.")
      vehicleTypeOpt = Some(v.toLowerCase.capitalize)
  })

  opt("d", "data", "<string>", "Comma separated data columns to be plotted along annotation (occupancy, vehicles, avg_speed).", {
    v: String =>
      val d = v.split(",")
      dataOpt = Option {
        Try(d.map(_.trim.toLowerCase)) getOrElse fatal("Please specify a valid set of data columns, e.g. occupancy,vehicles.")
      }
  })

  opt("pdf", "pdf-filename", "<string>", "Specify a filename for the pdf file, e.g. output.pdf.", {
    v: String =>
      if(!v.matches(".*[.]pdf")) fatal("Please specify a valid filename, e.g. output.pdf.")
      pdfOpt = Option {
        Try(v) getOrElse fatal("Please specify a valid filename, e.g. output.pdf.")
      }
  })

  opt("png", "png-filename", "<string>", "Specify a filename for the png image file, e.g. output.png.", {
    v: String =>
      if(!v.matches(".*[.]png")) fatal("Please specify a valid filename, e.g. output.png.")
      pngOpt = Option {
        Try(v) getOrElse fatal("Please specify a valid filename, e.g. output.png.")
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

  val simulationId = simulationIdOpt getOrElse fatal("Please specify the simulation id")
  val trafficLevel = trafficLevelOpt getOrElse fatal("Please specify the traffic level")
  val sectionId = sectionIdOpt getOrElse fatal("Please specify the section id")
  val vehicleType = vehicleTypeOpt getOrElse fatal("Please specify the vehicle type")

  // Check if given data columns exist in the input table
  val columns = dataOpt.getOrElse(fatal("Please specify a set of data columns"))
  columns.foreach { column =>
    if(!InputData.baseTableRow.columnNames.contains(s"simulation_input.$column"))
      fatal(s"Data column $column does not exist in the input table")
  }

  // --- 1. Visualize the given data columns for the given time interval
  visualize(simulationId, trafficLevel, sectionId, vehicleType, columns, pdfOpt, pngOpt)

  // --- 2. Close database connection
  closeConnection()

  private def visualize(simulationId: Int, trafficLevel: String, locationId: Int,
                        vehicleType: String, columns: Seq[String], pdfName: Option[String],
                        pngName: Option[String]) = {

    // Find the maximum time point in the input dataset
    val maximum =
      blockingExec {
        InputData
        .filter(i => i.trafficLevel === trafficLevel &&
          i.simulationId === simulationId &&
          i.vehicleType === vehicleType)
        .map(i => i.timeStamp).max.result
    } getOrElse fatal("There is no maximum time point!")

    val annotation = Array.fill(maximum)(0.0)

    val annotationQuery =
      AnnotationData
        .filter(a => a.trafficLevel === trafficLevel &&
          a.simulationId === simulationId &&
          a.sectionId === locationId)

    val annotatedLocationQuery =
      LocationData.join(annotationQuery)
        .on(_.sectionId === _.sectionId)

    val annotatedLocations = blockingExec {
        annotatedLocationQuery.result
      }

    if (annotatedLocations.isEmpty)
      fatal("Annotated location does not exist on locations table!")
    else
      annotatedLocations.foreach { case (a, b) =>
        (b.startTs to b.endTs).foreach(ts => annotation(ts - 1) = 100.0)
      }

    val inputQuery =
      InputData
        .filter(i =>
          i.trafficLevel === trafficLevel &&
            i.simulationId === simulationId &&
            i.vehicleType === vehicleType)

    // Print data
    blockingExec {
      inputQuery.join(annotatedLocationQuery)
        .on((a, b) => a.detectorId === b._1.detectorId && a.timeStamp > 0).result
    }.foreach { case (a, b) =>
      println(a.timeStamp + ", " + a.vehicles.getOrElse(0.0) + ", " + a.avgSpeed.getOrElse(0.0) + ", " + a.occupancy.getOrElse(0.0))
    }

    val data = columns.map {

      case "occupancy" =>
        val occupancyArray = Array.fill(maximum)(0.0)
        blockingExec {
          inputQuery.join(annotatedLocationQuery).on((a, b) => a.detectorId === b._1.detectorId && a.timeStamp > 0).result
        }.foreach { case (a, b) =>
          occupancyArray(a.timeStamp - 1) = a.occupancy.getOrElse(0.0)
        }
        (occupancyArray, "Occupancy")

      case "vehicles" =>
        val vehiclesArray = Array.fill(maximum)(0.0)
        blockingExec {
          inputQuery.join(annotatedLocationQuery).on((a, b) => a.detectorId === b._1.detectorId && a.timeStamp > 0).result
        }.foreach { case (a, b) =>
          vehiclesArray(a.timeStamp - 1) = a.vehicles.getOrElse(0.0)
        }
        (vehiclesArray, "Vehicles")

      case "avg_speed" =>
        val avgSpeedArray = Array.fill(maximum)(0.0)
        blockingExec {
          inputQuery.join(annotatedLocationQuery).on((a, b) => a.detectorId === b._1.detectorId && a.timeStamp > 0).result
        }.foreach { case (a, b) =>
          avgSpeedArray(a.timeStamp - 1) = a.avgSpeed.getOrElse(0.0)
        }
        (avgSpeedArray, "Average Speed")
    }

    // Time domain
    val time = (1 to maximum).map(_.toDouble)

    val datasets = Seq((time zip annotation, "Annotation")) ++ data.map(d => (time zip d._1, d._2))

    if (pdfName.isDefined)
      plotPDF(datasets, "Time", "Data", pdfName.get)
    else if (pngName.isDefined)
      plotImage(datasets, "Time", "Data", pngName.get)
    else
      plot(datasets, s"${columns.map(_.capitalize).mkString(",")} in $locationId", "Time", "Data")
  }

}
