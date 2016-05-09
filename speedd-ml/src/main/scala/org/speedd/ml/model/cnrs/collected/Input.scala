package org.speedd.ml.model.cnrs.collected

import slick.driver.PostgresDriver.api._

/**
  * Entity `Input`
  *
  * @param locId location id
  * @param lane lane type
  * @param timeStamp timestamp
  * @param occupancy occupancy number
  * @param vehicles number of vehicles
  * @param avgSpeed average speed
  */
case class Input(locId: Long,
                 lane: String,
                 timeStamp: Int,
                 occupancy: Option[Double] = None,
                 vehicles: Option[Int] = None,
                 avgSpeed: Option[Double] = None)

class InputTable (tag: Tag) extends Table[Input] (tag, Some("cnrs"), "input") {

  def locId = column[Long]("lod_id")
  def lane = column[String]("lane")
  def timeStamp = column[Int]("time_stamp")
  def occupancy = column[Option[Double]]("occupancy")
  def vehicles = column[Option[Int]]("vehicles")
  def avgSpeed = column[Option[Double]]("avg_speed")

  def pk = primaryKey("pk_input", (locId, lane, timeStamp))

  def * = (locId, lane, timeStamp, occupancy, vehicles, avgSpeed) <> (Input.tupled, Input.unapply)

  def indexInput = index("idx_input", timeStamp)
}
