package org.speedd.ml.loaders.cnrs.simulation.highway

import java.io.File
import org.speedd.ml.loaders.DataLoader
import org.speedd.ml.model.cnrs.simulation.highway.{Input, InputData}
import org.speedd.ml.util.data.CSV
import org.speedd.ml.util.data.DatabaseManager._
import slick.driver.PostgresDriver.api._
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success}
import org.speedd.ml.util.data._

/**
  * Loads and converts input highway data from CSV files. The data was simulated by CNRS.
  *
  * <p>
  * The expected format of the CSV file is the following:
  * <ul>
  *   <li>simulation_time: unique simulation id, which shows the random seed (integer)</li>
  *   <li>detector_id: represents the detector id (integer)</li>
  *   <li>vehicle_speed: represents the average speed of the counter vehicles (double)</li>
  *   <li>vehicles_count_car: represents the counted number of car vehicles (double)</li>
  *   <li>vehicles_count_trunk: represents the counted number of trunk vehicles (double)</li>
  *   <li>density: represents the density of vehicles (double)</li>
  *   <li>density_car: represents the density of car vehicles (double)</li>
  *   <li>density_trunk: represents the density of trunk vehicles (double)</li>
  *   <li>occupancy: represents the percentage of time the sensor is occupied (double)</li>
  * </ul>
  * </p>
  */
object InputDataLoader extends DataLoader {

  private val DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss"
  private val DATE_TIME_FORMAT_SHORT = "yy-MM-dd HH:mm"

  /**
    * Loads all data from a sequence of CSV files into the database.
    *
    * @param inputFiles a sequence of files
    */
  override def loadAll(inputFiles: Seq[File]) = {

    val pattern = "simulator_data_incident([0-9]+)[\\.[[csv]|[gz]|[zip]]]*".r

    info("Loading sensor input data")

    InputData.createSchema()

    var futureList = List[Future[Option[Int]]]()

    inputFiles.filter(f => f.isFile && f.canRead).
      foreach { file =>
        info(s"Parsing file '${file.getName}'")
        val parser = CSV.parseIterator(file, skipHeader = true) match {
          case Success(csvParser) => csvParser
          case Failure(ex) => fatal(ex.getMessage)
        }

        var stop = false
        val pattern(simulationId) = file.getName
        while(!stop) {
          CSV.parseNextBatch[Input](parser, toInput, externalFields = Some(Array(simulationId))) match {
            case Success(result) => futureList +:= asyncExec(InputData ++= result)
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
  private def toInput(source: Array[String]): Option[Input] = {

    implicit def doubleToOpt(number: Double): Option[Double] = {
      if(number < 0.0) None
      else Some(number)
    }

    implicit def intToOpt(number: Int): Option[Int] = {
      if(number < 0) None
      else Some(number)
    }

    val timeStamp = ts2UnixTS(source(0), DATE_TIME_FORMAT, DATE_TIME_FORMAT_SHORT, startTs = 1450281615, round = true)

    /*
     * Note that occupancy, vehicles and avgSpeed are implicitly converted to Option instances, when their values are
     * below zero are considered as `None`, otherwise instantiated as Some(value).
     */
    Some(Input(source(9).toInt, timeStamp, source(1).toInt, source(2).toDouble, source(3).toDouble, source(4).toDouble,
      source(5).toDouble, source(6).toDouble, source(7).toDouble, source(8).toDouble))
  }
}
