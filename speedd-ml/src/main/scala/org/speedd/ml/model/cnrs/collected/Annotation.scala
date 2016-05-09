package org.speedd.ml.model.cnrs.collected

import slick.driver.PostgresDriver.api._

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

class AnnotationTable(tag: Tag) extends Table[Annotation] (tag, Some("cnrs"), "annotation") {

  def startTs = column[Int]("start_ts")
  def endTs = column[Int]("end_ts")
  def eventNum = column[Int]("event_num")
  def decription = column[String]("description")
  def sensor = column[Int]("sensor")
  def startLoc = column[Int]("start_loc")
  def endLoc = column[Int]("end_loc")

  def pk = primaryKey("pk_annotation", (startTs, endTs, eventNum))

  def * = (startTs, endTs, eventNum, decription, sensor, startLoc, endLoc) <> (Annotation.tupled, Annotation.unapply)

  def indexAnnotation = index("idx_annotation", decription)
}

object AnnotationTable {
  lazy val table = TableQuery[AnnotationTable]
}