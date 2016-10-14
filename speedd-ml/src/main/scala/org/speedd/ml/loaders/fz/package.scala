package org.speedd.ml.loaders

import lomrf.mln.model._
import org.speedd.ml.model.fz.InputData
import org.speedd.ml.util.data.DatabaseManager._
import slick.driver.PostgresDriver.api._
import org.speedd.ml.util.data._

package object fz {

  def loadFor(simulationId: Option[Int] = None,
              startTs: Int, endTs: Int,
              initial: ConstantsDomain = Map.empty,
              useOnlyConstants: DomainMap = Map.empty): (DomainMap, AnnotationTuples[Int, String, Int, Int]) = {

    var domainsMap = initial.map(pair => pair._1 -> pair._2.toIterable)

    // Append all time points in the given interval
    domainsMap += "timestamp" -> (startTs to endTs).map(_.toString).toIterable

    val cardsInfo = blockingExec {
      InputData.filter(i => i.timeStamp >= startTs && i.timeStamp <= endTs)
        .map(i => (i.cardPan, i.cardExpDate)).result
    }.map { case (cardPan, cardExpDate) =>
      (cardPan, cardExpDate.toString)
    }.unzip

    domainsMap ++= Iterable("card_pan" -> cardsInfo._1.distinct, "card_exp_date" -> cardsInfo._2.distinct)

    val annotationIntervalQuery =
      InputData.filter(i => i.timeStamp.between(startTs, endTs))

    val annotationTuples = blockingExec {
      annotationIntervalQuery.map(a => (a.timeStamp, a.cardPan, a.cardExpDate, a.isFraud)).result
    }

    (domainsMap, annotationTuples)
  }
}
