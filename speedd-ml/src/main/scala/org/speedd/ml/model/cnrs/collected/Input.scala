package org.speedd.ml.model.cnrs.collected

import slick.driver.PostgresDriver.api._
import org.speedd.ml.util.data.DatabaseManager._
import slick.jdbc.meta.MTable

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

class InputTable (tag: Tag) extends Table[Input] (tag, Some("cnrs"), "collected_input") {

  def locId = column[Long]("loc_id")
  def lane = column[String]("lane")
  def timeStamp = column[Int]("timestamp")
  def occupancy = column[Option[Double]]("occupancy")
  def vehicles = column[Option[Int]]("vehicles")
  def avgSpeed = column[Option[Double]]("avg_speed")

  def pk = primaryKey("pk_collected_input", (locId, lane, timeStamp))

  def * = (locId, lane, timeStamp, occupancy, vehicles, avgSpeed) <> (Input.tupled, Input.unapply)

  def indexInput = index("idx_collected_input", timeStamp)

  lazy val columnNames = List(locId.toString, lane.toString, timeStamp.toString,
                              occupancy.toString, vehicles.toString, avgSpeed.toString)
}

object InputData extends TableQuery[InputTable](new InputTable(_)) {

  def createSchema() =
    if (blockingExec {
      MTable.getTables("collected_input")
    }.isEmpty) blockingExec(this.schema.create)
}