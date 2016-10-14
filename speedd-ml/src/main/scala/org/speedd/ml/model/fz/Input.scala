package org.speedd.ml.model.fz

import slick.driver.PostgresDriver.api._
import org.speedd.ml.util.data.DatabaseManager._
import slick.jdbc.meta.MTable

/**
  * Entity `Input`
  *
  * @param timeStamp transaction timestamp
  * @param transactionId transaction id
  * @param isCNP true if card not present, otherwise false
  * @param amount the amount of the transaction in euros
  * @param cardPan hashed card PAN
  * @param cardExpDate card expiration date
  * @param cardCountry card country code
  * @param cardFamily card family (VISA, MasterCard, etc)
  * @param cardType card type inside VISA or MasterCard
  * @param cardTech card technologies, such as chip or band
  * @param acquirerCountry country of the acquiring bank
  * @param merchantMCC merchant category code reflecting its business area
  * IGNORE terminalBrand
  * @param terminalId internal identification of the terminal
  * @param terminalType type of the terminal, such as an ATM or a POS (point of sale)
  * @param terminalEmv indicates whether the terminal supports EMV or not
  * @param transactionResponse the response to this transaction processing. It can be accepted or rejected
  * @param cardAuth reflect the type of the authentication available for the card (band or chip)
  * @param terminalAuth reflect the type of the authentication available for the terminal (keyed-in band or chip)
  * @param clientAuth reflect the type of the authentication available for the client (PIN, mail or telephone)
  * IGNORE cardBrand
  * @param cvvValidation indicates whether the CVV was used or not, and in the positive case, indicates if there was any anomaly
  * IGNORE tmpCardPan
  * IGNORE tmpCardExpDate
  * @param transactionType the type of the transaction (recurring, single, etc)
  * IGNORE AuthType
  * @param isFraud indicates whether this transaction was marked as fraud or not (annotation)
  */
case class Input(timeStamp: Int,
                 transactionId: String,
                 isCNP: Int,
                 amount: Double,
                 cardPan: String,
                 cardExpDate: Int,
                 cardCountry: Int,
                 cardFamily: Int,
                 cardType: Int,
                 cardTech: Int,
                 acquirerCountry: Int,
                 merchantMCC: Int,
                 terminalId: Long,
                 terminalType: Int,
                 terminalEmv: Int,
                 transactionResponse: Int,
                 cardAuth: Int,
                 terminalAuth: Int,
                 clientAuth: Int,
                 cvvValidation: Int,
                 transactionType: Int,
                 isFraud: Int)

class InputTable (tag: Tag) extends Table[Input] (tag, Some("fz"), "input") {

  def timeStamp = column[Int]("timestamp")
  def transactionId = column[String]("transaction_id")
  def isCNP = column[Int]("is_cnp")
  def amount = column[Double]("amount")
  def cardPan = column[String]("card_pan")
  def cardExpDate = column[Int]("card_exp_date")
  def cardCountry = column[Int]("card_country")
  def cardFamily = column[Int]("card_family")
  def cardType = column[Int]("card_type")
  def cardTech = column[Int]("card_tech")
  def acquirerCountry = column[Int]("acquirer_country")
  def merchantMCC = column[Int]("merchant_mcc")
  def terminalId = column[Long]("terminal_id")
  def terminalType = column[Int]("terminal_type")
  def terminalEmv = column[Int]("terminal_emv")
  def transactionResponse = column[Int]("transaction_response")
  def cardAuth = column[Int]("card_auth")
  def terminalAuth = column[Int]("terminal_auth")
  def clientAuth = column[Int]("client_auth")
  def cvvValidation = column[Int]("cvv_validation")
  def transactionType = column[Int]("transaction_type")
  def isFraud = column[Int]("is_fraud")

  def pk = primaryKey("pk_input", (timeStamp, transactionId))

  def * = (timeStamp, transactionId, isCNP, amount, cardPan, cardExpDate, cardCountry, cardFamily, cardType, cardTech,
    acquirerCountry, merchantMCC, terminalId, terminalType, terminalEmv, transactionResponse, cardAuth, terminalAuth,
    clientAuth, cvvValidation, transactionType, isFraud) <> (Input.tupled, Input.unapply)

  def indexInput = index("idx_input", timeStamp)

}

object InputData extends TableQuery[InputTable](new InputTable(_)) {

  def createSchema() =
    if (blockingExec {
      MTable.getTables("input")
    }.isEmpty) blockingExec(this.schema.create)
}
