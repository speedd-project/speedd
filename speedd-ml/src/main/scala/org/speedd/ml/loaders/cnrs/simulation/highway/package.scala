package org.speedd.ml.loaders.cnrs.simulation

import lomrf.mln.model.ConstantsDomain
import org.speedd.ml.model.cnrs.simulation.highway.{AnnotationData, LocationData}
import org.speedd.ml.util.data.DatabaseManager._
import org.speedd.ml.util.data._
import slick.driver.PostgresDriver.api._

package object highway {

  def loadFor(simulationId: Int, startTs: Int, endTs: Int,
              initial: ConstantsDomain = Map.empty): (DomainMap, AnnotationTuples[Int, Int, Int, String]) = {

    var domainsMap = initial.map(pair => pair._1 -> pair._2.toIterable)

    // Append all time points in the given interval
    domainsMap += "timestamp" -> (startTs to endTs).map(_.toString)

    // Append all section ids (locations)
    domainsMap += "section_id" -> blockingExec {
      LocationData.map(l => l.sectionId).result
    }.map(_.toString)

    val annotationIntervalQuery =
      AnnotationData
        .filter(a => a.simulationId === simulationId &&
        a.startTs <= endTs && a.endTs >= startTs)

    LocationData.join(annotationIntervalQuery)
      .on(_.sectionId === _.sectionId)

    /*
     * Creates annotated location tuples for each location id existing in the database
     * table `simulation_highway_location`. It performs join in order to keep only the
     * section ids that annotation exists. Then it expands the annotation intervals and
     * keeps only those time-points belonging into the current batch interval.
     * Finally if no annotation interval exists for a specific location id, lane pair then
     * for all time-points of the current batch their `description` column is set to None.
     */
    val annotationTuples = blockingExec {
      annotationIntervalQuery.result
    }.flatMap { a =>
      (a.startTs to a.endTs)
        .filter(ts => ts >= startTs && ts <= endTs)
        .map(ts => (ts, a.simulationId, a.sectionId, a.description))
    }

    (domainsMap, annotationTuples)
  }
}
