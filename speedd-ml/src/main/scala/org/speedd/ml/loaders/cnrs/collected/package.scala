package org.speedd.ml.loaders.cnrs

import lomrf.mln.model._
import org.speedd.ml.model.cnrs.collected.{AnnotationData, InputData, LocationData}
import org.speedd.ml.util.data.DatabaseManager._
import slick.driver.PostgresDriver.api._
import org.speedd.ml.util.data._

package object collected {

  /**
    * PR0 to PR11 relative distances in meters from PR0.
    */
  val prDistances = Array(0, 962, 1996, 2971, 3961, 4961, 5948, 6960, 7971, 8961, 9966, 11035)

  /**
    * User defined occupancy levels
    */
  val occLevels = Array(0.0, 30.0, 101.0)
  //val occLevels = Array(0.0, 10.0, 20.0, 30.0, 40.0, 50.0, 60.0, 70.0, 80.0, 90.0, 100.0)

  /**
    * User defined average speed levels
    */
  val speedLevels = Array(0.0, 55.0, 100.0)
  //val speedLevels = Array(0.0, 10.0, 20.0, 30.0, 40.0, 50.0, 60.0, 70.0, 80.0, 90.0, 100.0)

  /**
    * User defined vehicle numbers
    */
  val vehicleLevels = Array(0.0, 4.0, 8.0, 16.0, 32.0)

  /**
    * Symbols to domains
    */
  val symbols2domain = Map("O" -> "occupancy", "S" -> "avg_speed", "V" -> "vehicles")

  /**
    * Mapping of column name to user defined function
    */
  val domain2udf = Map(
    "occupancy" -> mkSymbolic(occLevels, "O"),
    "avg_speed" -> mkSymbolic(speedLevels, "S"),
    "vehicles" -> mkSymbolic(vehicleLevels, "V")
  )

  /**
    * Mapping of symbol names to user defined function producing intervals
    */
  val symbols2udf = Map(
    "O" -> mkInterval(occLevels, symbols2domain),
    "S" -> mkInterval(speedLevels, symbols2domain),
    "V" -> mkInterval(vehicleLevels,symbols2domain)
  )

  def loadFor(startTs: Int, endTs: Int,
              initial: ConstantsDomain = Map.empty) = {

    var domainsMap = initial.map(pair => pair._1 -> pair._2.toIterable)

    domainsMap += "timestamp" -> (startTs to endTs).map(_.toString)

    val inputValues =
      blockingExec {
        InputData.filter(i => i.timeStamp >= startTs && i.timeStamp <= endTs)
          .map(i => (i.occupancy, i.vehicles, i.avgSpeed)).result
      }.unzip3

    domainsMap ++= Iterable("occupancy" -> inputValues._1.flatten.map(domain2udf("occupancy")(_)).distinct,
      "vehicles" -> inputValues._2.flatten.map(domain2udf("vehicles")(_)).distinct,
      "avg_speed" -> inputValues._3.flatten.map(domain2udf("avg_speed")(_)).distinct)

    val locationValues = blockingExec {
      LocationData.map(l => (l.locId, l.lane)).result
    }.map{ case (locId, lane) =>
      (locId.toString, lane)
    }.unzip

    domainsMap ++= Iterable("loc_id" -> locationValues._1.distinct,
      "lane" -> locationValues._2.distinct)

    val annotationIntervalQuery =
      AnnotationData.filter(a => a.startTs <= endTs && a.endTs >= startTs)

    domainsMap += "description" -> blockingExec {
      annotationIntervalQuery.map(_.description).result
    }.distinct

    /*
     * Creates annotated location tuples for each pair of location id and lane existing
     * in the database table `location`. It performs left join in order to keep all pairs
     * of location id, lane regardless of annotation existence. Then it expands the annotation
     * intervals and keeps only those time-points belonging into the current batch interval.
     * Finally if no annotation interval exists for a specific location id, lane pair then
     * for all time-points of the current batch their `description` column is set to None.
     */
    val annotationTuples = blockingExec {
      LocationData.map(l => (l.locId, l.distance, l.lane))
        .joinLeft(annotationIntervalQuery)
        .on((a, b) => a._2 <= b.startLoc && a._2 >= b.endLoc).distinct.result
    }.map { case (loc, ann) =>
      if (ann.isDefined)
        (Some(ann.get.startTs), Some(ann.get.endTs), loc._1, loc._3, Some(ann.get.description))
      else (None, None, loc._1, loc._3, None)
    }.flatMap { case (startT, endT, locId, lane, description) =>
      (startTs to endTs).map { ts =>
        if (startT.isDefined && endT.isDefined && ts >= startT.get && ts <= endT.get) (ts, locId, lane, description)
        else (ts, locId, lane, None)
      }
    }

    (domainsMap, annotationTuples)
  }
}
