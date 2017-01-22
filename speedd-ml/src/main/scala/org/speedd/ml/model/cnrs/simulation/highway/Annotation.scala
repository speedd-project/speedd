package org.speedd.ml.model.cnrs.simulation.highway

import org.speedd.ml.util.data.DatabaseManager._
import slick.driver.PostgresDriver.api._
import slick.jdbc.meta.MTable

/**
  * Entity `Annotation`
  *
  * @param simulationId unique simulation id
  * @param sectionId section id
  * @param startTs start timestamp
  * @param endTs end timestamp
  * @param effectLength length of road is blocked due to accident
  * @param description the target label
  */
case class Annotation(simulationId: Int,
                      sectionId: Int,
                      startTs: Int,
                      endTs: Int,
                      effectLength: Double,
                      description: String)

class AnnotationTable(tag: Tag) extends Table[Annotation] (tag, Some("cnrs"), "simulation_highway_annotation") {

  def simulationId = column[Int]("simulation_id")
  def sectionId = column[Int]("section_id")
  def startTs = column[Int]("start_ts")
  def endTs = column[Int]("end_ts")
  def effectLength = column[Double]("effect_length")
  def description = column[String]("description")

  def pk = primaryKey("pk_simulation_highway_annotation", simulationId)

  def * = (simulationId, sectionId, startTs, endTs, effectLength, description) <> (Annotation.tupled, Annotation.unapply)

  def indexAnnotation = index("idx_simulation_highway_annotation", description)
}

object AnnotationData extends TableQuery[AnnotationTable](new AnnotationTable(_)) {

  def createSchema() =
    if (blockingExec {
      MTable.getTables("simulation_highway_annotation")
    }.isEmpty) blockingExec(this.schema.create)
}
