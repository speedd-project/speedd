package org.speedd.ml.app

import org.speedd.ml.model.cnrs.simulation._
import org.speedd.ml.util.data.Plotter._
import slick.driver.PostgresDriver.api._

object SimulatedCNRSDataPlotApp extends CLIDataPlotApp {

  // -------------------------------------------------------------------------------------------------------------------
  // --- Configuration parameters
  // -------------------------------------------------------------------------------------------------------------------
  private var taskOpt: Option[String] = None
  private var simulationIdOpt: Option[Int] = None
  private var locationIdOpt: Option[Int] = None
  private var trafficLevelOpt: Option[String] = None
  private var vehicleTypeOpt: Option[String] = None
  private var printData: Boolean = false

  // -------------------------------------------------------------------------------------------------------------------
  // --- Command line interface options
  // -------------------------------------------------------------------------------------------------------------------
  intOpt("sid", "simulation-id", "<integer>", "Simulation id data to be plotted (1 to 10).", {
    v: Int =>
      if (v < 1 || v > 10) fatal("Please specify a valid simulation id, e.g. 1 to 10.")
      simulationIdOpt = Some(v)
  })

  intOpt("lid", "location-id", "<integer>", "The location id data to plot.", {
    v: Int =>
      locationIdOpt = Some(v)
  })

  opt("level", "traffic-level", "<string>", "Level of traffic (low or high).", {
    v: String =>
      if (!v.toLowerCase.matches("low|high")) fatal("Please specify a valid level of traffic e.g. low or high.")
      trafficLevelOpt = Some(v.toLowerCase.capitalize)
  })

  opt("type", "vehicle-type", "<string>", "Plot data for specific vehicles types (all, car or trunk).", {
    v: String =>
      if(!v.toLowerCase.matches("all|car|trunk")) fatal("Please specify a vehicle type e.g. all, car or trunk.")
      vehicleTypeOpt = Some(v.toLowerCase.capitalize)
  })

  opt("t", "task", "Specify one of the following tasks: simulation.city, simulation.highway", {
    v: String => taskOpt = Some(v.trim.toLowerCase)
  })

  flagOpt("printData", "print-data", "Print data columns for each time point in the plot interval", {
    printData = true
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

  import org.speedd.ml.util.data.DatabaseManager._

  val task = taskOpt.getOrElse(fatal("Please specify a task"))

  // Check if the simulation id is given
  val simulationId = simulationIdOpt getOrElse fatal("Please specify a simulation id")

  // Check if the traffic level is required or not
  if (trafficLevelOpt.isDefined && task == "simulation.highway")
    warn(s"Traffic level is not required for task '$task'")

  // Check if the vehicle type is required or not
  if (vehicleTypeOpt.isDefined && task == "simulation.highway")
    warn(s"Vehicle type is not required for task '$task'")

  // Check if the given location id exists in the database for the specified task
  val locationId = locationIdOpt getOrElse fatal("Please specify a location id")

  if (task == "simulation.city") if (blockingExec {
    city.LocationData.filter(_.sectionId === locationId).result
  }.isEmpty) fatal(s"Location id '$locationId' does not exist in the database for task $task")

  else if (task == "simulation.highway") if (blockingExec {
    highway.LocationData.filter(_.sectionId === locationId).result
  }.isEmpty) fatal(s"Location id '$locationId' does not exist in the database for task $task")

  else fatal(s"Unknown task '$task', please set one of the following tasks:\n(1) simulation.city\n(2) simulation.highway")

  // Check if given data columns exist in the input table of the specified task
  val columns = dataOpt.getOrElse(fatal("Please specify a set of data columns"))

  if (task == "simulation.city") columns.foreach { column =>
    if(!city.InputData.baseTableRow.columnNames.contains(s"simulation_city_input.$column"))
      fatal(s"Data column '$column' does not exist in the input table of the task $task")
  }

  else if (task == "simulation.highway") columns.foreach { column =>
    if(!highway.InputData.baseTableRow.columnNames.contains(s"simulation_highway_input.$column"))
      fatal(s"Data column '$column' does not exist in the input table of the task $task")
  }

  else fatal(s"Unknown task '$task', please set one of the following tasks:\n(1) simulation.city\n(2) simulation.highway")

  // --- 1. Visualize the given data columns for the given time interval
  visualize(task, intervalOpt, slidingOpt, simulationId, locationId, columns, pdfOpt, pngOpt,
    rangeOpt, printData, trafficLevelOpt, vehicleTypeOpt)

  // --- 2. Close database connection
  closeConnection()

  private def visualize(task: String, interval: Option[(Int, Int)], sliding: Option[Int],
                        simulationId: Int, locationId: Int, columns: Seq[String], pdfName: Option[String],
                        pngName: Option[String], range: Option[(Double, Double)], printData: Boolean,
                        trafficLevel: Option[String] = None, vehicleType: Option[String] = None) = {

    task match {
      case "simulation.city" =>
        val trafficLevel = trafficLevelOpt getOrElse fatal("Please specify the traffic level")
        val vehicleType = vehicleTypeOpt getOrElse fatal("Please specify the vehicle type")

        // Check if a temporal interval has been specified. Else plot the data for all time points.
        val (startTs, endTs) =
          intervalOpt.getOrElse {
            warn(s"Interval not specified. Plotting data for over all time points.")
            val time =
              blockingExec {
                city.InputData
                  .filter(i => i.trafficLevel === trafficLevel &&
                    i.simulationId === simulationId &&
                    i.vehicleType === vehicleType && i.timeStamp > 0)
                  .map(i => i.timeStamp).result
              }
            (time.min, time.max)
          }

        // Load annotation data points
        val annotation = Array.fill(endTs - startTs + 1)(0.0)

        val annotationQuery =
          city.AnnotationData
            .filter(a => a.trafficLevel === trafficLevel &&
              a.simulationId === simulationId &&
              a.sectionId === locationId && a.startTs <= endTs && a.endTs >= startTs)

        val annotatedLocationQuery =
          city.LocationData.join(annotationQuery)
            .on(_.sectionId === _.sectionId)

        blockingExec {
          annotatedLocationQuery.result
        }.foreach { case (loc, ann) =>
          (ann.startTs to ann.endTs)
            .filter(ts => ts >= startTs && ts <= endTs)
            .foreach(ts => annotation(ts - startTs) = 100.0)
        }

        // Input data query for the specified simulation id, traffic level, vehicles type and interval
        val inputQuery =
          city.InputData
            .filter(i =>
              i.trafficLevel === trafficLevel &&
                i.simulationId === simulationId &&
                i.vehicleType === vehicleType && i.timeStamp.between(startTs, endTs))

        // Print data
        if (printData) blockingExec {
          inputQuery.join(city.LocationData)
            .on((a, b) => a.detectorId === b.detectorId && b.sectionId === locationId).sortBy(_._1.timeStamp).result
        }.foreach { case (a, b) =>
          info(s"${a.timeStamp}, ${a.vehicles.getOrElse(0.0)}, ${a.avgSpeed.getOrElse(0.0)}, ${a.occupancy.getOrElse(0.0)}")
        }

        // Load data
        val data = columns.map {

          case "occupancy" =>
            val occupancyArray = Array.fill(endTs - startTs + 1)(0.0)
            blockingExec {
              inputQuery.join(city.LocationData)
                .on((a, b) => a.detectorId === b.detectorId && b.sectionId === locationId).result
            }.foreach { case (input, _) =>
              occupancyArray(input.timeStamp - startTs) = input.occupancy.getOrElse(0.0)
            }
            (occupancyArray, "Occupancy")

          case "vehicles" =>
            val vehiclesArray = Array.fill(endTs - startTs + 1)(0.0)
            blockingExec {
              inputQuery.join(city.LocationData)
                .on((a, b) => a.detectorId === b.detectorId && b.sectionId === locationId).result
            }.foreach { case (input, _) =>
              vehiclesArray(input.timeStamp - startTs) = input.vehicles.getOrElse(0.0)
            }
            (vehiclesArray, "Vehicles")

          case "avg_speed" =>
            val avgSpeedArray = Array.fill(endTs - startTs + 1)(0.0)
            blockingExec {
              inputQuery.join(city.LocationData)
                .on((a, b) => a.detectorId === b.detectorId && b.sectionId === locationId).result
            }.foreach { case (input, _) =>
              avgSpeedArray(input.timeStamp - startTs) = input.avgSpeed.getOrElse(0.0)
            }
            (avgSpeedArray, "Average Speed")
        }

        // Check if all data points are zero
        if (data.forall(_._1.forall(_ == 0.0)))
          fatal(s"There are no data points available for id$simulationId and '${trafficLevel.toLowerCase}'" +
            s" traffic in location '$locationId' for the interval '$startTs to $endTs'.")

        // Time domain
        val time = (startTs to endTs).map(_.toDouble)

        val datasets =
          if (annotation.forall(_ == 0.0)) {
            warn(s"Annotation does not exist for id$simulationId and '${trafficLevel.toLowerCase}'" +
              s" traffic in location '$locationId' for the interval '$startTs to $endTs'.")
            data.map(d => (time zip d._1, d._2))
          }
          else
            Seq((time zip annotation, "Annotation")) ++ data.map(d => (time zip d._1, d._2))

        val (lb, ub) =
          if (range.isDefined)
            (Some(range.get._1), Some(range.get._2))
          else (None, None)

        if (pdfName.isDefined)
          plotPDF(datasets, "Time", "Data", pdfName.get, lbRange = lb, ubRange = ub)
        else if (pngName.isDefined)
          plotImage(datasets, "Time", "Data", pngName.get, lbRange = lb, ubRange = ub)

        else sliding match {
          case Some(window) =>
            slidingPlot(datasets, window, s"${columns.map(c => c.replace("_",". ").capitalize).mkString(", ")} in $locationId", "Time", "Data", lbRange = lb, ubRange = ub)
          case None =>
            plot(datasets, s"${columns.map(_.capitalize).mkString(",")} in $locationId", "Time", "Data", lbRange = lb, ubRange = ub)
        }

      case "simulation.highway" =>

        // Check if a temporal interval has been specified. Else plot the data for all time points.
        val (startTs, endTs) =
          intervalOpt.getOrElse {
            warn(s"Interval not specified. Plotting data for over all time points.")
            val time =
              blockingExec {
                highway.InputData
                  .filter(i => i.simulationId === simulationId)
                  .map(i => i.timeStamp).result
              }
            (time.min, time.max)
          }

        // Load annotation data points
        val annotation = Array.fill(endTs - startTs + 1)(0.0)

        val annotationQuery =
          highway.AnnotationData
            .filter(a => a.simulationId === simulationId &&
              a.sectionId === locationId && a.startTs <= endTs && a.endTs >= startTs)

        val annotatedLocationQuery =
          highway.LocationData.join(annotationQuery)
            .on(_.sectionId === _.sectionId)

        blockingExec {
          annotatedLocationQuery.result
        }.foreach { case (loc, ann) =>
          (ann.startTs to ann.endTs)
            .filter(ts => ts >= startTs && ts <= endTs)
            .foreach(ts => annotation(ts - startTs) = 100.0)
        }

        val inputQuery =
          highway.InputData.filter(i => i.simulationId === simulationId && i.timeStamp.between(startTs, endTs))

        // Print data
        if (printData) blockingExec {
          inputQuery.join(highway.LocationData)
            .on((a, b) => a.detectorId === b.detectorId && b.sectionId === locationId).sortBy(_._1.timeStamp).result
        }.foreach { case (a, b) =>
          info(s"${a.timeStamp}, ${a.avgSpeed.getOrElse(0.0)}, ${a.carVehicles.getOrElse(0.0)}," +
            s" ${a.trunkVehicles.getOrElse(0.0)}, ${a.density.getOrElse(0.0)}, ${a.carDensity.getOrElse(0.0)}," +
            s"${a.trunkDensity.getOrElse(0.0)}, ${a.occupancy.getOrElse(0.0)}")
        }

        // Load data
        val data = columns.map {

          case "occupancy" =>
            val occupancyArray = Array.fill(endTs - startTs + 1)(0.0)
            blockingExec {
              inputQuery.join(highway.LocationData)
                .on((a, b) => a.detectorId === b.detectorId && b.sectionId === locationId).result
            }.foreach { case (input, _) =>
              occupancyArray(input.timeStamp - startTs) = input.occupancy.getOrElse(0.0)
            }
            (occupancyArray, "Occupancy")

          case "car_vehicles" =>
            val vehiclesArray = Array.fill(endTs - startTs + 1)(0.0)
            blockingExec {
              inputQuery.join(highway.LocationData)
                .on((a, b) => a.detectorId === b.detectorId && b.sectionId === locationId).result
            }.foreach { case (input, _) =>
              vehiclesArray(input.timeStamp - startTs) = input.carVehicles.getOrElse(0.0)
            }
            (vehiclesArray, "Car Vehicles")

          case "trunk_vehicles" =>
            val vehiclesArray = Array.fill(endTs - startTs + 1)(0.0)
            blockingExec {
              inputQuery.join(highway.LocationData)
                .on((a, b) => a.detectorId === b.detectorId && b.sectionId === locationId).result
            }.foreach { case (input, _) =>
              vehiclesArray(input.timeStamp - startTs) = input.trunkVehicles.getOrElse(0.0)
            }
            (vehiclesArray, "Trunk Vehicles")

          case "density" =>
            val densityArray = Array.fill(endTs - startTs + 1)(0.0)
            blockingExec {
              inputQuery.join(highway.LocationData)
                .on((a, b) => a.detectorId === b.detectorId && b.sectionId === locationId).result
            }.foreach { case (input, _) =>
              densityArray(input.timeStamp - startTs) = input.density.getOrElse(0.0)
            }
            (densityArray, "Density")

          case "car_density" =>
            val densityArray = Array.fill(endTs - startTs + 1)(0.0)
            blockingExec {
              inputQuery.join(highway.LocationData)
                .on((a, b) => a.detectorId === b.detectorId && b.sectionId === locationId).result
            }.foreach { case (input, _) =>
              densityArray(input.timeStamp - startTs) = input.carDensity.getOrElse(0.0)
            }
            (densityArray, "Car Density")

          case "trunk_density" =>
            val densityArray = Array.fill(endTs - startTs + 1)(0.0)
            blockingExec {
              inputQuery.join(highway.LocationData)
                .on((a, b) => a.detectorId === b.detectorId && b.sectionId === locationId).result
            }.foreach { case (input, _) =>
              densityArray(input.timeStamp - startTs) = input.trunkDensity.getOrElse(0.0)
            }
            (densityArray, "Trunk Density")

          case "avg_speed" =>
            val avgSpeedArray = Array.fill(endTs - startTs + 1)(0.0)
            blockingExec {
              inputQuery.join(highway.LocationData)
                .on((a, b) => a.detectorId === b.detectorId && b.sectionId === locationId).result
            }.foreach { case (input, _) =>
              avgSpeedArray(input.timeStamp - startTs) = input.avgSpeed.getOrElse(0.0)
            }
            (avgSpeedArray, "Average Speed")
        }

        // Check if all data points are zero
        if (data.forall(_._1.forall(_ == 0.0)))
          fatal(s"There are no data points available for id$simulationId" +
            s" in location '$locationId' for the interval '$startTs to $endTs'.")

        // Time domain
        val time = (startTs to endTs).map(_.toDouble)

        val datasets =
          if (annotation.forall(_ == 0.0)) {
            warn(s"Annotation does not exist for id$simulationId" +
              s" in location '$locationId' for the interval '$startTs to $endTs'.")
            data.map(d => (time zip d._1, d._2))
          }
          else
            Seq((time zip annotation, "Annotation")) ++ data.map(d => (time zip d._1, d._2))

        val (lb, ub) =
          if (range.isDefined)
            (Some(range.get._1), Some(range.get._2))
          else (None, None)

        if (pdfName.isDefined)
          plotPDF(datasets, "Time", "Data", pdfName.get, lbRange = lb, ubRange = ub)
        else if (pngName.isDefined)
          plotImage(datasets, "Time", "Data", pngName.get, lbRange = lb, ubRange = ub)

        else sliding match {
          case Some(window) =>
            slidingPlot(datasets, window, s"${columns.map(c => c.replace("_",". ").capitalize).mkString(", ")} in $locationId", "Time", "Data", lbRange = lb, ubRange = ub)
          case None =>
            plot(datasets, s"${columns.map(_.capitalize).mkString(",")} in $locationId", "Time", "Data", lbRange = lb, ubRange = ub)
        }
    }
  }

}
