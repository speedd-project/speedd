package org.speedd.ml.loaders.cnrs.simulation.city

import java.io.File
import org.speedd.ml.loaders.DataLoader
import org.speedd.ml.model.cnrs.simulation.city.{Input, InputData}
import org.speedd.ml.util.data.CSV
import org.speedd.ml.util.data.DatabaseManager._
import slick.driver.PostgresDriver.api._
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success}

/**
  * Loads and converts input city data from CSV files. The data was simulated by CNRS.
  *
  * <p>
  * The expected format of the CSV file is the following:
  * <ul>
  *   <li>did: unique simulation id, which shows the random seed (integer)</li>
  *   <li>oid: represents the detector id (integer)</li>
  *   <li>sid: represents the vehicle types (0 = for all vehicle type, 1 = car, 2 = truck)</li>
  *   <li>ent: represents the time interval of the simulation time (integer)</li>
  *   <li>count_veh: represents the counted number of vehicles (double)</li>
  *   <li>speedd: represents the average speed of the counter vehicles (double)</li>
  *   <li>occupancy: represents the percentage of time the sensor is occupied (double)</li>
  * </ul>
  * </p>
  */
object InputDataLoader extends DataLoader {

  val pattern = "data_accident_(high|low)_sim([0-9]*)[\\.[[csv]|[gz]|[zip]]]*".r

  /**
    * Loads all data from a sequence of CSV files into the database.
    *
    * @param inputFiles a sequence of files
    */
  override def loadAll(inputFiles: Seq[File]) = {
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
        val pattern(trafficLevel, simulationId) = file.getName
        while(!stop) {
          CSV.parseNextBatch[Input](parser, toInput, externalFields = Some(Array(simulationId, trafficLevel.capitalize))) match {
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
  private def toInput(source: Array[String]): Option[Input] ={

    implicit def doubleToOpt(number: Double): Option[Double] = {
      if(number < 0.0) None
      else Some(number)
    }

    implicit def intToOpt(number: Int): Option[Int] = {
      if(number < 0) None
      else Some(number)
    }

    val vehicleType = if (source(2).toInt == 0) "All" else if (source(2).toInt == 1) "Car" else "Trunk"

    /*
     * Note that occupancy, vehicles and avgSpeed are implicitly converted to Option instances, when their values are
     * below zero are considered as `None`, otherwise instantiated as Some(value).
     */
    Some(Input(source(7).toInt, source(8), source(3).toInt, source(1).toInt, vehicleType,
      source(4).toDouble, source(5).toDouble, source(6).toDouble))
  }
}
