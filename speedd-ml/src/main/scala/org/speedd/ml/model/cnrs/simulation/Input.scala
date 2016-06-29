package org.speedd.ml.model.cnrs.simulation

import slick.driver.PostgresDriver.api._
import org.speedd.ml.util.data.DatabaseManager._
import slick.jdbc.meta.MTable

/**
  * Entity `Input`
  *
  * @param simulationId unique simulation id
  * @param detectorId detector id
  * @param vehicleType vehicle types (0 = all vehicles, 1 = car, 2 = truck)
  * @param timeStamp timestamp
  * @param trafficLevel level of traffic (Low or High)
  * @param vehicles number of vehicles
  * @param avgSpeed average speed
  * @param occupancy occupancy
  */
case class Input(simulationId: Int,
                 trafficLevel: String,
                 timeStamp: Int,
                 detectorId: Int,
                 vehicleType: String,
                 vehicles: Option[Double] = None,
                 avgSpeed: Option[Double] = None,
                 occupancy: Option[Double] = None)

class InputTable (tag: Tag) extends Table[Input] (tag, Some("cnrs"), "simulation_input") {

  def simulationId = column[Int]("simulation_id")
  def trafficLevel = column[String]("traffic_level")
  def timeStamp = column[Int]("timestamp")
  def detectorId = column[Int]("detector_id")
  def vehicleType = column[String]("vehicle_type")
  def vehicles = column[Option[Double]]("vehicles")
  def avgSpeed = column[Option[Double]]("avg_speed")
  def occupancy = column[Option[Double]]("occupancy")

  def pk = primaryKey("pk_simulation_input", (simulationId, trafficLevel, timeStamp, detectorId, vehicleType))

  def * = (simulationId, trafficLevel, timeStamp, detectorId, vehicleType, vehicles, avgSpeed, occupancy) <> (Input.tupled, Input.unapply)

  def indexInput = index("idx_simulation_input", timeStamp)

  lazy val columnNames = List(simulationId.toString, trafficLevel.toString, timeStamp.toString, detectorId.toString,
    vehicleType.toString, vehicles.toString, avgSpeed.toString, occupancy.toString)
}

object InputData extends TableQuery[InputTable](new InputTable(_)) {

  def createSchema() =
    if (blockingExec {
      MTable.getTables("simulation_input")
    }.isEmpty) blockingExec(this.schema.create)
}
