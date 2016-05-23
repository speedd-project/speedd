package org.speedd.ml.app

import auxlib.log.Logging
import auxlib.opt.OptionParser
import org.speedd.ml.ModuleVersion
import org.speedd.ml.model.cnrs.collected.{AnnotationData, InputData, LocationData}
import scala.util.Try
import slick.driver.PostgresDriver.api._
import org.speedd.ml.util.data.Plotter._

object CNRSDataPlotApp extends App with OptionParser with Logging {

  println(s"${ModuleVersion()}\nData Plot Application")

  // -------------------------------------------------------------------------------------------------------------------
  // --- Configuration parameters
  // -------------------------------------------------------------------------------------------------------------------
  private var dataOpt: Option[Seq[String]] = None
  private var intervalOpt: Option[(Int, Int)] = None
  private var slidingOpt: Option[Int] = None
  private var locationIdOpt: Option[Long] = None
  private var laneOpt: Option[String] = None
  private var pdfOpt: Option[String] = None

  // -------------------------------------------------------------------------------------------------------------------
  // --- Command line interface options
  // -------------------------------------------------------------------------------------------------------------------

  opt("d", "data", "<string>", "Comma seperated data columns to be plotted along annotation (occupancy, vehicles, avg_speed).", {
    v: String =>
      val d = v.split(",")
      dataOpt = Option {
        Try(d.map(_.trim.toLowerCase)) getOrElse fatal("Please specify a valid set of data columns, e.g. occupancy,vehicles.")
      }
  })

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

  opt("i", "interval", "<start time-point>,<end time-point>", "Specify the temporal interval for plotting data, e.g. 10,100.", {
    v: String =>
      val t = v.split(",")
      if(t.length != 2) fatal("Please specify a valid temporal interval, e.g. 10,100.")
      else intervalOpt = Option {
        Try((t(0).toInt, t(1).toInt)) getOrElse fatal("Please specify a valid temporal interval, e.g. 10,100.")
      }
  })

  opt("sw", "sliding-window", "<integer>", "Specify a sliding window for data visualization, e.g. 100.", {
    v: String =>
      slidingOpt = Option {
        Try(v.toInt) getOrElse fatal("Please specify a valid sliding window, e.g. 1000.")
      }
  })

  opt("pdf", "pdf-filename", "<string>", "Specify a filename for the pdf file, e.g. output.pdf.", {
    v: String =>
      if(!v.matches(".*[.]pdf")) fatal("Please specify a valid filename, e.g. output.pdf.")
      pdfOpt = Option {
        Try(v) getOrElse fatal("Please specify a valid filename, e.g. output.pdf.")
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
    if(!InputData.baseTableRow.columnNames.contains(s"input.$column"))
      fatal(s"Data column $column does not exist in the input table")
  }

  // --- 1. Visualize the given data columns for the given time interval
  visualize(startTime, endTime, slidingOpt, locationId, lane, columns, pdfOpt)

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
                        locationId: Long, lane: String, columns: Seq[String], pdfName: Option[String]) = {

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
          occupancyArray(timeStamp - startTs) = occupancy.get
        }
        (occupancyArray, "Occupancy")

      case "vehicles" =>
        val vehiclesArray = Array.fill(endTs - startTs + 1)(0.0)
        blockingExec {
          basicQuery.map(i => (i.timeStamp, i.vehicles)).result
        }.foreach { case (timeStamp, vehicles) =>
          vehiclesArray(timeStamp - startTs) = vehicles.get
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

    if (pdfName.isDefined)
      plotPDF(datasets, "Time", "Data", pdfName.get)

    else sliding match {
      case Some(window) =>
        slidingPlot(datasets, window, s"${columns.map(c => c.replace("_",". ").capitalize).mkString(", ")} in $locationId, $lane", "Time", "Data")
      case None =>
        plot(datasets, s"${columns.map(_.capitalize).mkString(",")} in $locationId, $lane", "Time", "Data")
    }
  }

  /*private def plotOccupancy(startTs: Int, endTs: Int, sliding: Option[Int], locationId: Long, lane: String) = {

    val annotationArray = Array.fill(endTs - startTs + 1)(0.0)
    val occupancyArray = Array.fill(endTs - startTs + 1)(0.0)

    blockingExec {
      InputData.filter(i => i.timeStamp >= startTs && i.timeStamp <= endTs && i.locId === locationId && i.lane === lane)
        .map(i => (i.timeStamp, i.occupancy)).result
    }.foreach { case (timeStamp, occupancy) =>
      occupancyArray(timeStamp - startTs) = occupancy.get
    }

    val annotationIntervalQuery =
      AnnotationData.filter(a => a.startTs <= endTs && a.endTs >= startTs)

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
        if (ts >= start && ts <= end) annotationArray(ts - startTs) = 105.0
      }
    }

    val time = (startTs to endTs).map(_.toDouble)

    sliding match {
      case Some(window) =>
        slidingPlot(List((time zip occupancyArray, "Occupancy"), (time zip annotationArray, "Annotation")), window,
          s"Occupancy in $locationId, $lane", "Time", "Occupancy/Annotation")
      case None =>
        plot(List((time zip occupancyArray, "Occupancy"), (time zip annotationArray, "Annotation")),
          s"Occupancy in $locationId, $lane", "Time", "Occupancy/Annotation")
    }
  }

  def plotVehicles(startTs: Int, endTs: Int, sliding: Option[Int], locationId: Long, lane: String) = {

    val annotationArray = Array.fill(endTs - startTs + 1)(0.0)
    val vehiclesArray = Array.fill(endTs - startTs + 1)(0.0)

    blockingExec {
      InputData.filter(i => i.timeStamp >= startTs && i.timeStamp <= endTs && i.locId === locationId && i.lane === lane)
        .map(i => (i.timeStamp, i.vehicles)).result
    }.foreach { case (timeStamp, vehicles) =>
      vehiclesArray(timeStamp - startTs) = vehicles.get
    }

    val annotationIntervalQuery =
      AnnotationData.filter(a => a.startTs <= endTs && a.endTs >= startTs)

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
        if (ts >= start && ts <= end) annotationArray(ts - startTs) = 20.0
      }
    }

    val time = (startTs to endTs).map(_.toDouble)

    sliding match {
      case Some(window) =>
        slidingPlot(List((time zip vehiclesArray, "Vehicles"), (time zip annotationArray, "Annotation")), window,
          s"Vehicles in $locationId, $lane", "Time", "Vehicles/Annotation")
      case None =>
        plot(List((time zip vehiclesArray, "Vehicles"), (time zip annotationArray, "Annotation")),
          s"Vehicles in $locationId, $lane", "Time", "Vehicles/Annotation")
    }
  }

  def plotAvgSpeed(startTs: Int, endTs: Int, sliding: Option[Int], locationId: Long, lane: String) = {

    val annotationArray = Array.fill(endTs - startTs + 1)(0.0)
    val avgSpeedArray = Array.fill(endTs - startTs + 1)(0.0)

    blockingExec {
      InputData.filter(i => i.timeStamp >= startTs && i.timeStamp <= endTs && i.locId === locationId && i.lane === lane)
        .map(i => (i.timeStamp, i.avgSpeed)).result
    }.foreach { case (timeStamp, avgSpeed) =>
      avgSpeedArray(timeStamp - startTs) = avgSpeed.getOrElse(0.0)
    }

    val annotationIntervalQuery =
      AnnotationData.filter(a => a.startTs <= endTs && a.endTs >= startTs)

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
        if (ts >= start && ts <= end) annotationArray(ts - startTs) = 200.0
      }
    }

    val time = (startTs to endTs).map(_.toDouble)

    sliding match {
      case Some(window) =>
        slidingPlot(List((time zip avgSpeedArray, "Average Speed"), (time zip annotationArray, "Annotation")), window,
          s"Avg. speed in $locationId, $lane", "Time", "Avg.Speed/Annotation")
      case None =>
        plot(List((time zip avgSpeedArray, "Average Speed"), (time zip annotationArray, "Annotation")),
          s"Avg. speed in $locationId, $lane", "Time", "Avg.Speed/Annotation")
    }
  }*/
}
