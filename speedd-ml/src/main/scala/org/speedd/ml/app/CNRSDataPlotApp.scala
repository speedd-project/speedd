package org.speedd.ml.app

import java.awt.Paint

import auxlib.log.Logging
import auxlib.opt.OptionParser
import org.jfree.chart.{ChartFactory, ChartPanel}
import org.jfree.chart.plot.PlotOrientation
import org.jfree.data.xy
import org.jfree.data.xy.{XYDataset, XYSeries, XYSeriesCollection}
import org.jfree.ui.{ApplicationFrame, RefineryUtilities}
import org.speedd.ml.ModuleVersion
import org.speedd.ml.model.cnrs.collected.{annotation, input, location}
import scala.util.Try
import slick.driver.PostgresDriver.api._
import org.speedd.ml.util.data.Plotter._

object CNRSDataPlotApp extends App with OptionParser with Logging {

  println(s"${ModuleVersion()}\nData Plot Application")

  // -------------------------------------------------------------------------------------------------------------------
  // --- Configuration parameters
  // -------------------------------------------------------------------------------------------------------------------
  private var taskOpt: Option[String] = None
  private var intervalOpt: Option[(Int, Int)] = None
  private var locationIdOpt: Option[Long] = None
  private var laneOpt: Option[String] = None

  // -------------------------------------------------------------------------------------------------------------------
  // --- Command line interface options
  // -------------------------------------------------------------------------------------------------------------------

  opt("t", "task", "<string>", "The name of the task to call (occupancy or vehicles or avgSpeed).", {
    v: String => taskOpt = Some(v.trim.toLowerCase)
  })

  opt("loc", "location-id", "<integer>", "The location id data to plot.", {
    v: String => locationIdOpt = Some(v.toLong)
  })

  opt("lane", "lane", "<string>", "The lane of the location.", {
    v: String => laneOpt = Some(v.trim)
  })

  opt("i", "interval", "<start time-point>,<end time-point>", "Specify the temporal interval for training, e.g. 10,100 ", {
    v: String =>
      val t = v.split(",")
      if(t.length != 2) fatal("Please specify a valid temporal interval, e.g. 10,100")
      else intervalOpt = Option {
        Try((t(0).toInt, t(1).toInt)) getOrElse fatal("Please specify a valid temporal interval. For example: 10,100")
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

  // The temporal interval by which we will plot the data
  val (startTime, endTime) = intervalOpt getOrElse fatal("Please specify an interval")
  val intervalLength = endTime - startTime

  val locationId = locationIdOpt getOrElse fatal("Please specify a location id")
  val lane = laneOpt getOrElse fatal("Please specify a lane")

  import org.speedd.ml.util.data.DatabaseManager._

  // --- 1. Create the appropriate instance of inference engine
  taskOpt.getOrElse(fatal("Please specify a task name")) match {
    case "occupancy" => plotOccupancy(startTime, endTime, locationId, lane)
    case _ => fatal("Please specify a task name")
  }

  // --- 2. Close database connection
  closeConnection()

  private def plotOccupancy(startTs: Int, endTs: Int, locationId: Long, lane: String) = {

    val annotationArray = Array.fill(endTs - startTs + 1)(0.0)
    val occupancyArray = Array.fill(endTs - startTs + 1)(0.0)

    blockingExec {
      input.filter(i => i.timeStamp >= startTs && i.timeStamp <= endTs && i.locId === locationId && i.lane === lane)
        .map(i => (i.timeStamp, i.occupancy)).result
    }.foreach { case (timeStamp, occupancy) =>
      occupancyArray(timeStamp - startTs) = occupancy.get
    }

    val annotationIntervalQuery =
      annotation.filter(a => a.startTs <= endTs && a.endTs >= startTs)

    /*
     * Creates annotated location tuples for each pair of location id and lane existing
     * in the database table `location`. It performs left join in order to keep all pairs
     * of location id, lane regardless of annotation existence. Then it expands the annotation
     * intervals and keeps only those time-points belonging into the current batch interval.
     * Finally if no annotation interval exists for a specific location id, lane pair then
     * for all time-points of the current batch their `description` column is set to None.
     */
    blockingExec {
      location.filter(l => l.locId === locationId)
        .join(annotationIntervalQuery)
        .on((a, b) => a.distance <= b.startLoc && a.distance >= b.endLoc)
        .map(joined => (joined._2.startTs, joined._2.endTs, joined._1.locId)).distinct.result
    }.foreach { case (start, end, locId) =>
      (startTs to endTs).foreach { ts =>
        if (ts >= start && ts <= end) annotationArray(ts - startTs) = 105.0
      }
    }

    val time = (startTs to endTs).map(_.toDouble)

    plot(List((time zip occupancyArray, "Occupancy"), (time zip annotationArray, "Annotation")),
      s"Occupancy in $locationId, $lane", "Time", "Occupancy/Annotation")
  }
}
