package org.speedd.ml.model.cnrs.simulation.city

import org.speedd.ml.util.data.DatabaseManager._
import slick.driver.PostgresDriver.api._
import slick.jdbc.meta.MTable

/**
  * Entity `Location`
  *
  * @param detectorId detector id
  * @param sectionId section id
  * @param location location name
  */
case class Location(detectorId: Int,
                    sectionId: Int,
                    location: Option[String] = None)

class LocationTable(tag: Tag) extends Table[Location] (tag, Some("cnrs"), "simulation_city_location") {

  def detectorId = column[Int]("detector_id")
  def sectionId = column[Int]("section_id")
  def location = column[Option[String]]("location")

  def pk = primaryKey("pk_simulation_city_location", detectorId)

  def * = (detectorId, sectionId, location) <> (Location.tupled, Location.unapply)
}

object LocationData extends TableQuery[LocationTable](new LocationTable(_)) {

  def createSchema() =
    if (blockingExec {
      MTable.getTables("simulation_city_location")
    }.isEmpty) blockingExec(this.schema.create)
}

