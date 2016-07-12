package org.speedd.ml.app

import auxlib.log.Logging
import auxlib.opt.OptionParser
import org.speedd.ml.ModuleVersion
import scala.util.Try

/**
  * Data plot application interface. Should be implemented by all command line applications
  * intended for plotting data from a database.
  */
trait CLIDataPlotApp extends App with OptionParser with Logging {

  info(s"${ModuleVersion()}\nData Plot Application")

  // -------------------------------------------------------------------------------------------------------------------
  // --- Configuration parameters
  // -------------------------------------------------------------------------------------------------------------------
  protected var dataOpt: Option[Seq[String]] = None
  protected var intervalOpt: Option[(Int, Int)] = None
  protected var slidingOpt: Option[Int] = None
  protected var rangeOpt: Option[(Double, Double)] = None
  protected var pdfOpt: Option[String] = None
  protected var pngOpt: Option[String] = None

  // -------------------------------------------------------------------------------------------------------------------
  // --- Command line interface options
  // -------------------------------------------------------------------------------------------------------------------
  opt("d", "data", "<string>", "Comma separated data columns to be plotted along annotation (if any exists).", {
    v: String =>
      val columns = v.split(",")
      dataOpt = Option {
        Try(columns.map(_.trim.toLowerCase)) getOrElse fatal("Please specify a valid set of data columns.")
      }
  })

  opt("i", "interval", "<start time-point>,<end time-point>", "Specify the temporal interval for plotting data, e.g. 1,500.", {
    v: String =>
      val interval = v.split(",")
      if(interval.length != 2) fatal("Please specify a valid temporal interval, e.g. 1,500.")
      else intervalOpt = Option {
        Try((interval(0).toInt, interval(1).toInt)) getOrElse fatal("Please specify a valid temporal interval, e.g. 1,500.")
      }
  })

  opt("sw", "sliding-window", "<integer>", "Specify a sliding window for data visualization, e.g. 100.", {
    v: String =>
      slidingOpt = Option {
        Try(v.toInt) getOrElse fatal("Please specify a valid sliding window, e.g. 100.")
      }
  })

  opt("r", "range", "<double>,<double>", "Specify the range of the figure (y-axis), e.g. 0.1,5.2.", {
    v: String =>
      val range = v.split(",")
      if(range.length != 2) fatal("Please specify a valid range for the figure, e.g. 0.1,5.2.")
      else rangeOpt = Option {
        Try((range(0).toDouble, range(1).toDouble)) getOrElse fatal("Please specify a valid range for the figure, e.g. 0.1,5.2.")
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
}
