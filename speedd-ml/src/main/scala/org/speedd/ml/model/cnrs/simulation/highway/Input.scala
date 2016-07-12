package org.speedd.ml.model.cnrs.simulation.highway

import org.speedd.ml.util.data.DatabaseManager._
import slick.driver.PostgresDriver.api._
import slick.jdbc.meta.MTable

/**
  * Entity `Input`
  *
  * @param simulationId unique simulation id
  * @param detectorId detector id
  * @param timeStamp timestamp
  * @param avgSpeed average speed
  * @param carVehicles number of car vehicles
  * @param trunkVehicles number of trunk vehicles
  * @param density density of vehicles
  * @param carDensity density of car vehicles
  * @param trunkDensity density of trunk vehicles
  * @param occupancy occupancy
  */
case class Input(simulationId: Int,
                 timeStamp: Int,
                 detectorId: Int,
                 avgSpeed: Option[Double] = None,
                 carVehicles: Option[Double] = None,
                 trunkVehicles: Option[Double] = None,
                 density: Option[Double] = None,
                 carDensity: Option[Double] = None,
                 trunkDensity: Option[Double] = None,
                 occupancy: Option[Double] = None)

class InputTable (tag: Tag) extends Table[Input] (tag, Some("cnrs"), "simulation_highway_input") {

  def simulationId = column[Int]("simulation_id")
  def timeStamp = column[Int]("timestamp")
  def detectorId = column[Int]("detector_id")
  def avgSpeed = column[Option[Double]]("avg_speed")
  def carVehicles = column[Option[Double]]("car_vehicles")
  def trunkVehicles = column[Option[Double]]("trunk_vehicles")
  def density = column[Option[Double]]("density")
  def carDensity = column[Option[Double]]("car_density")
  def trunkDensity = column[Option[Double]]("trunk_density")
  def occupancy = column[Option[Double]]("occupancy")

  def pk = primaryKey("pk_simulation_highway_input", (simulationId, timeStamp, detectorId))

  def * = (simulationId, timeStamp, detectorId, avgSpeed, carVehicles, trunkVehicles,
    density, carDensity, trunkDensity, occupancy) <> (Input.tupled, Input.unapply)

  def indexInput = index("idx_simulation_highway_input", timeStamp)

  lazy val columnNames = List(simulationId.toString, timeStamp.toString, detectorId.toString, avgSpeed.toString,
    carVehicles.toString, trunkVehicles.toString, density.toString, carDensity.toString, trunkDensity.toString,
    occupancy.toString)
}

object InputData extends TableQuery[InputTable](new InputTable(_)) {

  def createSchema() =
    if (blockingExec {
      MTable.getTables("simulation_highway_input")
    }.isEmpty) blockingExec(this.schema.create)
}