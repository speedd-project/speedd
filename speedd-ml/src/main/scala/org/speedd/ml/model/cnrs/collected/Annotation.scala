package org.speedd.ml.model.cnrs.collected

import org.speedd.ml.util.data.DatabaseManager._
import slick.driver.PostgresDriver.api._
import slick.jdbc.meta.MTable

/**
  * Entity `Annotation`
  *
  * @param startTs start timestamp
  * @param endTs end timestamp
  * @param eventNum event number
  * @param description the target label
  * @param sensor sensor type
  * @param startLoc location starting point
  * @param endLoc location ending point
  */
case class Annotation(startTs: Int,
                      endTs: Int,
                      eventNum: Int,
                      description: String,
                      sensor: Int,
                      startLoc: Int,
                      endLoc: Int)

class AnnotationTable(tag: Tag) extends Table[Annotation] (tag, Some("cnrs"), "collected_annotation") {

  def startTs = column[Int]("start_ts")
  def endTs = column[Int]("end_ts")
  def eventId = column[Int]("event_id")
  def description = column[String]("description")
  def sensor = column[Int]("sensor")
  def startLoc = column[Int]("start_loc")
  def endLoc = column[Int]("end_loc")

  def pk = primaryKey("pk_collected_annotation", (startTs, endTs, eventId))

  def * = (startTs, endTs, eventId, description, sensor, startLoc, endLoc) <> (Annotation.tupled, Annotation.unapply)

  def indexAnnotation = index("idx_collected_annotation", description)
}

object AnnotationData extends TableQuery[AnnotationTable](new AnnotationTable(_)) {

  def createSchema() =
    if (blockingExec {
      MTable.getTables("collected_annotation")
    }.isEmpty) blockingExec(this.schema.create)
}