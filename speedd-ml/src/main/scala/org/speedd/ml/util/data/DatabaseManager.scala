package org.speedd.ml.util.data

import slick.driver.PostgresDriver.api._
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

object DatabaseManager {

  private lazy val db = Database.forConfig("speeddDB")

  /**
    * Asynchronous execution of a given action.
    *
    * @param action a given action to execute
    *
    * @return a future
    */
  def asyncExec[T](action: DBIO[T]): Future[T] =
    db.run(action)

  /**
    * Blocking execution of a given action. The execution blocks by
    * default for infinite duration, that is until it completes.
    *
    * @param action a given action to execute
    * @param duration the blocking duration (default is infinite)
    *
    * @return a result
    */
  def blockingExec[T](action: DBIO[T], duration: Duration = Duration.Inf): T =
    Await.result(db.run(action), duration)

  /**
    * Creates a schema in the database having the given name.
    *
    * @param schema the name of the schema
    * @return
    */
  def createSchema(schema: String) = blockingExec {
    sql"CREATE SCHEMA IF NOT EXISTS #$schema AUTHORIZATION postgres;".asUpdate
  }

  /**
    * Closes the connection to the database. Then connection is initialized
    * during the first database action execution.
    */
  def closeConnection() = db.close()
}
