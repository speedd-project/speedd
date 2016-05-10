package org.speedd.ml.loaders.cnrs.collected

import java.io.File
import java.sql.Timestamp

import org.speedd.ml.loaders.DataLoader
import org.speedd.ml.model.cnrs.collected.{Input, InputTable, input}
import org.speedd.ml.util.data.CSV
import slick.driver.PostgresDriver.api._
import org.speedd.ml.util.data.DatabaseManager._
import slick.driver.PostgresDriver

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success}

object InputDataLoader extends DataLoader {

  override def loadAll(inputFiles: Seq[File]) = {
    info("Loading sensor input data")

    exec(input.createSchema())

    var futureList = List[Future[Option[Int]]]()

    inputFiles.filter(f => f.isFile && f.canRead).
      foreach { file =>
        info(s"Parsing file '${file.getName}'")
        val parser = CSV.parseIterator(file)
        var stop = false
        while(!stop) {
          CSV.parseNextBatch[Input](parser, toInput) match {
            case Success(result) => futureList +:= exec(input ++= result)
            case Failure(ex) => stop = true
          }
        }
      }

    futureList.foreach(f => Await.result(f, Duration.Inf))

  }

  private def toInput(source: Array[String]): Option[Input] ={

    implicit def doubleToOpt(number: Double): Option[Double] = {
      if(number <= 0.0) None
      else Some(number)
    }

    val lane = source(3).trim

    val START = 1396299600L
    val step = ((Timestamp.valueOf(source(0) + " " + source(1)).getTime / 1000).toInt + 7200 - START) / 15

    if(lane.isEmpty) None
    else {
      // Please note that the arguments occupancy, vehicles and avgSpeed are implicitly converted to Option[Double]
      // instances. When their values are equal or below zero are considered as `None`, otherwise are instantiated as
      // Some(value).
      Some(Input(
        // loc_id:
        java.lang.Long.valueOf(source(2), 16),
        // lane:
        lane.split(Array('-', ' ', '_')).map(_.trim.capitalize).reduce(_ + _),
        // timestamp (add 7200 to go +2 hours ahead, during to problem with annotation):
        (START + step).toInt,
        // occupancy:
        Some(source(4).toDouble),
        // vehicles:
        Some(source(5).toInt),
        // avg_speed:
        Some(source(7).toDouble)
      ))
    }
  }
}
