package org.speedd.ml.util.data

import slick.driver.PostgresDriver.api._

object DatabaseManager {

  private lazy val db = Database.forConfig("speeddDB")

  def exec[T](action: DBIO[T]) = db.run(action)

  def closeConnection() = db.close()

  def createCNRSSchema() = exec {
    sql"""CREATE SCHEMA IF NOT EXISTS cnrs AUTHORIZATION postgres;""".asUpdate
  }

  def createFZSchema() = exec {
    sql"""CREATE SCHEMA IF NOT EXISTS fz AUTHORIZATION postgres;""".asUpdate
  }

}
