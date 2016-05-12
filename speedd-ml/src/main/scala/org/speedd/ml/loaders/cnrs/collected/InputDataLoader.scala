package org.speedd.ml.loaders.cnrs.collected

import java.io.File
import org.speedd.ml.loaders.DataLoader
import org.speedd.ml.model.cnrs.collected.{Input, input}
import org.speedd.ml.util.data.CSV
import slick.driver.PostgresDriver.api._
import org.speedd.ml.util.data.DatabaseManager._
import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success}
import org.speedd.ml.util.data._

object InputDataLoader extends DataLoader {

  private val DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss"
  private val DATE_TIME_FORMAT_SHORT = "yy-MM-dd HH:mm"

  /**
    * Loads all data from a sequence of CSV files into the database.
    *
    * @param inputFiles a sequence of files
    */
  override def loadAll(inputFiles: Seq[File]) = {
    info("Loading sensor input data")

    input.createSchema()

    var futureList = List[Future[Option[Int]]]()

    inputFiles.filter(f => f.isFile && f.canRead).
      foreach { file =>
        info(s"Parsing file '${file.getName}'")
        val parser = CSV.parseIterator(file) match {
          case Success(csvParser) => csvParser
          case Failure(ex) => fatal(ex.getMessage)
        }
        var stop = false
        while(!stop) {
          CSV.parseNextBatch[Input](parser, toInput) match {
            case Success(result) => futureList +:= asyncExec(input ++= result)
            case Failure(ex) => stop = true
          }
        }
      }

    futureList.foreach(f => Await.result(f, Duration.Inf))

  }

  /**
    * Translator function used to map an array of strings produced by the CSV
    * parser into an `Input` object.
    *
    * @param source an array of strings
    * @return an Input object
    */
  private def toInput(source: Array[String]): Option[Input] ={

    implicit def doubleToOpt(number: Double): Option[Double] = {
      if(number < 0.0) None
      else Some(number)
    }

    implicit def intToOpt(number: Int): Option[Int] = {
      if(number < 0) None
      else Some(number)
    }

    val lane = source(3).trim

    // Add 7200 seconds to shift +2 hours ahead, during to problem with annotation
    val timeStamp = ts2UnixTS(source(0) + " " + source(1), DATE_TIME_FORMAT,
                    DATE_TIME_FORMAT_SHORT, 7200, 1396299600L, round = true)

    /*
     * Note that occupancy, vehicles and avgSpeed are implicitly converted to Option instances, when their values are
     * below zero are considered as `None`, otherwise instantiated as Some(value).
     */
    if(lane.isEmpty) None
    else Some(
      Input(
        // loc_id:
        java.lang.Long.valueOf(source(2), 16),
        // lane:
        lane.split(Array('-', ' ', '_')).map(_.trim.capitalize).reduce(_ + _),
        // timestamp:
        timeStamp,
        // occupancy:
        source(4).toDouble,
        // vehicles:
        source(5).toInt,
        // avg_speed:
        source(7).toDouble
      ))
  }
}
