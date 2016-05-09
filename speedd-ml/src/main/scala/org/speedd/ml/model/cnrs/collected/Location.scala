package org.speedd.ml.model.cnrs.collected

import slick.driver.PostgresDriver.api._

/**
  * Entity `Location`
  *
  * @param locId location id
  * @param lane lane type
  * @param prevLane previous lane type
  * @param coordinateX  x coordinate
  * @param coordinateY  y coordinate
  * @param num location number
  * @param distance the distance of the location
  */
case class Location(locId: Long,
                    lane: String,
                    prevLane: Option[String] = None,
                    coordinateX: Double,
                    coordinateY: Double,
                    num: Int,
                    distance: Int)


class LocationTable(tag: Tag) extends Table[Location] (tag, Some("cnrs"), "location") {

  def locId = column[Long]("lod_id")
  def lane = column[String]("lane")
  def prevLane = column[Option[String]]("prev_lane")
  def coordinateX = column[Double]("coordinate_x")
  def coordinateY = column[Double]("coordinate_y")
  def num = column[Int]("num")
  def distance = column[Int]("dist")

  def pk = primaryKey("pk_location", (locId, lane))

  def * = (locId, lane, prevLane, coordinateX, coordinateY, num, distance) <> (Location.tupled, Location.unapply)
}
