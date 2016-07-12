package org.speedd.ml.model.cnrs.simulation.highway

import org.speedd.ml.util.data.DatabaseManager._
import slick.driver.PostgresDriver.api._
import slick.jdbc.meta.MTable

/**
  * Entity `Location`
  *
  * @param detectorId detector id
  * @param sectionId section id
  */
case class Location(detectorId: Int,
                    sectionId: Int)

class LocationTable(tag: Tag) extends Table[Location] (tag, Some("cnrs"), "simulation_highway_location") {

  def detectorId = column[Int]("detector_id")
  def sectionId = column[Int]("section_id")

  def pk = primaryKey("pk_simulation_highway_location", detectorId)

  def * = (detectorId, sectionId) <> (Location.tupled, Location.unapply)
}

object LocationData extends TableQuery[LocationTable](new LocationTable(_)) {

  def createSchema() =
    if (blockingExec {
      MTable.getTables("simulation_highway_location")
    }.isEmpty) blockingExec(this.schema.create)
}
