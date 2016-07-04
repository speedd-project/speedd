package org.speedd.ml.app

import org.speedd.ml.model.cnrs.collected.{AnnotationData, InputData, LocationData}
import scala.util.Try
import slick.driver.PostgresDriver.api._
import org.speedd.ml.util.data.Plotter._

object CollectedCNRSDataPlotApp extends CLIDataPlotApp {

  // -------------------------------------------------------------------------------------------------------------------
  // --- Configuration parameters
  // -------------------------------------------------------------------------------------------------------------------
  private var locationIdOpt: Option[Long] = None
  private var laneOpt: Option[String] = None

  // -------------------------------------------------------------------------------------------------------------------
  // --- Command line interface options
  // -------------------------------------------------------------------------------------------------------------------

  opt("loc", "location-id", "<integer>", "The location id data to plot.", {
    v: String =>
      locationIdOpt = Option{
        Try(v.toLong) getOrElse fatal("Please specify a valid location id.")
      }
  })

  opt("lane", "location-lane", "<string>", "The lane for the given location id, e.g. Fast.", {
    v: String =>
      laneOpt = Option {
      Try(v) getOrElse fatal("Please specify a valid lane, e.g. Fast.")
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

  import org.speedd.ml.util.data.DatabaseManager._

  // The temporal interval by which we will plot the data
  val (startTime, endTime) = intervalOpt getOrElse fatal("Please specify an interval")
  val intervalLength = endTime - startTime

  // Check if the given location id exists in the database
  val locationId = locationIdOpt getOrElse fatal("Please specify a location id")
  if (blockingExec {
    LocationData.filter(_.locId === locationId).result
  }.isEmpty) fatal(s"Location id $locationId does not exist in the database")

  // Check if the given lane exists in the database for the given location id
  val lane = laneOpt getOrElse fatal("Please specify the lanes")
  if (blockingExec {
    LocationData.filter(l => l.locId === locationId && l.lane === lane).result
  }.isEmpty) fatal(s"Lane $lane does not exist in the database for location id $locationId")

  // Check if given data columns exist in the input table
  val columns = dataOpt.getOrElse(fatal("Please specify a set of data columns"))
  columns.foreach { column =>
    if(!InputData.baseTableRow.columnNames.contains(s"collected_input.$column"))
      fatal(s"Data column $column does not exist in the input table")
  }

  // --- 1. Visualize the given data columns for the given time interval
  visualize(startTime, endTime, slidingOpt, locationId, lane, columns, pdfOpt, pngOpt, rangeOpt)

  // --- 2. Close database connection
  closeConnection()

  private def loadAnnotation(startTs: Int, endTs: Int, locationId: Long): Array[Double] = {

    val annotation = Array.fill(endTs - startTs + 1)(0.0)

    val annotationIntervalQuery =
      AnnotationData.filter(a => a.startTs <= endTs && a.endTs >= startTs && a.description === "traffic_jam")

    /*
     * Creates annotated location tuples for each pair of location id and lane existing
     * in the database table `location`. It performs left join in order to keep all pairs
     * of location id, lane regardless of annotation existence. Then it expands the annotation
     * intervals and keeps only those time-points belonging into the current batch interval.
     * Finally if no annotation interval exists for a specific location id, lane pair then
     * for all time-points of the current batch their `description` column is set to None.
     */
    blockingExec {
      LocationData.filter(l => l.locId === locationId)
        .join(annotationIntervalQuery)
        .on((a, b) => a.distance <= b.startLoc && a.distance >= b.endLoc)
        .map(joined => (joined._2.startTs, joined._2.endTs, joined._1.locId)).distinct.result
    }.foreach { case (start, end, locId) =>
      (startTs to endTs).foreach { ts =>
        if (ts >= start && ts <= end) annotation(ts - startTs) = 100.0
      }
    }

    annotation
  }

  private def visualize(startTs: Int, endTs: Int, sliding: Option[Int],
                        locationId: Long, lane: String, columns: Seq[String],
                        pdfName: Option[String], pngName: Option[String],
                        range: Option[(Double, Double)]) = {

    // Time domain
    val time = (startTs to endTs).map(_.toDouble)

    // Load annotation for the given location id
    val annotation = loadAnnotation(startTs, endTs, locationId)

    // Basic query that filters the input table and keeps only relevant data
    val basicQuery = InputData.filter(i => i.timeStamp >= startTs && i.timeStamp <= endTs
                                      && i.locId === locationId && i.lane === lane)

    val data = columns.map {

      case "occupancy" =>
        val occupancyArray = Array.fill(endTs - startTs + 1)(0.0)
        blockingExec {
          basicQuery.map(i => (i.timeStamp, i.occupancy)).result
        }.foreach { case (timeStamp, occupancy) =>
          occupancyArray(timeStamp - startTs) = occupancy.getOrElse(0.0)
        }
        (occupancyArray, "Occupancy")

      case "vehicles" =>
        val vehiclesArray = Array.fill(endTs - startTs + 1)(0.0)
        blockingExec {
          basicQuery.map(i => (i.timeStamp, i.vehicles)).result
        }.foreach { case (timeStamp, vehicles) =>
          val k = vehicles.getOrElse(0)
          vehiclesArray(timeStamp - startTs) = k
        }
        (vehiclesArray, "Vehicles")

      case "avg_speed" =>
        val avgSpeedArray = Array.fill(endTs - startTs + 1)(0.0)
        blockingExec {
          basicQuery.map(i => (i.timeStamp, i.avgSpeed)).result
        }.foreach { case (timeStamp, avgSpeed) =>
          avgSpeedArray(timeStamp - startTs) = avgSpeed.getOrElse(0.0)
        }
        (avgSpeedArray, "Average Speed")
    }

    val datasets = Seq((time zip annotation, "Annotation")) ++ data.map(d => (time zip d._1, d._2))

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
        slidingPlot(datasets, window, s"${columns.map(c => c.replace("_",". ").capitalize).mkString(", ")} in $locationId, $lane", "Time", "Data", lbRange = lb, ubRange = ub)
      case None =>
        plot(datasets, s"${columns.map(_.capitalize).mkString(",")} in $locationId, $lane", "Time", "Data", lbRange = lb, ubRange = ub)
    }
  }
}
