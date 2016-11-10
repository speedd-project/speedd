package org.speedd.ml.loaders.fz

import java.io.File
import org.speedd.ml.loaders.DataLoader
import org.speedd.ml.model.fz.{Input, InputData}
import org.speedd.ml.util.data.CSV
import slick.driver.PostgresDriver.api._
import org.speedd.ml.util.data.DatabaseManager._
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success}

/**
  * Loads and converts input transaction data from CSV files. The data is collected and provided by FZ.
  *
  * <p>
  * The expected format of the CSV file is the following:
  * <ul>
  *   <li>timestamp: transaction timestamp (long)</li>
  *   <li>transaction_id: transaction id (long)</li>
  *   <li>is_cnp: cnp transaction indicator (0 or 1)</li>
  *   <li>amount_eur: transaction amount in euros (double)</li>
  *   <li>card_pan: hashed card pan (string)</li>
  *   <li>card_exp_date: card expiration date (date format YYYY MM)</li>
  *   <li>card_country: card country code (integer)</li>
  *   <li>card_family: card family (integer)</li>
  *   <li>card_type: card type (integer)</li>
  *   <li>card_tech: card technology support (integer)</li>
  *   <li>acquirer_country: acquirer bank country (integer)</li>
  *   <li>merchant_mcc: merchant category code (integer)</li>
  *   <li>terminal_brand: brand of the terminal’s manufacturer (long)</li>
  *   <li>terminal_id: internal identification of the terminal (long)</li>
  *   <li>terminal_type: the type of the terminal (integer)</li>
  *   <li>terminal_emv: indicates whether the terminal supports EMV or not (integer)</li>
  *   <li>transaction_response: the response to this transaction processing (integer)</li>
  *   <li>card_auth: reflects the type of the authentication available for the card (integer)</li>
  *   <li>terminal_auth: reflects the type of the authentication available for the terminal (integer)</li>
  *   <li>client_auth: reflects the type of the authentication available for the client (integer)</li>
  *   <li>card_brand: the brand of the card (integer)</li>
  *   <li>cvv_validation: indicates whether CVV was used or not, and if it was valid or there was any anomaly (integer)</li>
  *   <li>tmp_card_pan: pan of a virtual credit card, cards that do not exist physically (string)</li>
  *   <li>tmp_card_exp_date: expiration date of a virtual credit card (date format YYYY MM)</li>
  *   <li>transaction_type: the type of the transaction (integer)</li>
  *   <li>auth_type: refers to the 3D secure authentication type (integer)</li>
  *   <li>is_fraud: label indicating whether this transaction was marked as fraud or not (0 or 1)</li>
  * </ul>
  * </p>
  */
object InputDataLoader extends DataLoader {

  /**
    * Loads all data from a sequence of CSV files into the database.
    *
    * @param inputFiles a sequence of files
    */
  override def loadAll(inputFiles: Seq[File]) = {
    info("Loading transaction input data")

    InputData.createSchema()

    var futureList = List[Future[Option[Int]]]()

    inputFiles.filter(f => f.isFile && f.canRead).
      foreach { file =>
        info(s"Parsing file '${file.getName}'")
        val parser = CSV.parseIterator(file, skipHeader = true) match {
          case Success(csvParser) => csvParser
          case Failure(ex) => fatal(ex.getMessage)
        }
        var stop = false
        while(!stop) {
          CSV.parseNextBatch[Input](parser, toInput) match {
            case Success(result) => futureList +:= asyncExec(InputData ++= result)
            case Failure(ex) => stop = true
          }
        }
      }

    futureList.foreach(f => Await.result(f, Duration.Inf))
  }

  /**
    * Translator function used to map an array of strings produced by the CSV
    * parser into an `Input` object.
    *
    * @param source an array of strings
    * @return an Input object
    */
  private def toInput(source: Array[String]): Option[Input] = {
    Some(
      Input(
        source(0).toInt, source(1), source(2).toInt, source(3).toDouble, source(4), source(5).toInt,
        source(6).toInt, source(7).toInt, source(8).toInt, source(9).toInt, source(10).toInt, source(11).toInt,
        source(13).toLong, source(14).toInt, source(15).toInt, source(16).toInt, source(17).toInt, source(18).toInt,
        source(19).toInt, source(21).toInt, source(24).toInt, source(26).toInt
      )
    )
  }
}
