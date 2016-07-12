package org.speedd.ml.util

import java.text.SimpleDateFormat
import java.util.Arrays._

package object data {

  // A mapping of domain types to constant symbols
  type DomainMap = Map[String, Iterable[String]]

  // Annotation tuples retrieved from the database (up to 4 values)
  type AnnotationTuples[A, B, C, D] = Seq[(A, B, C, D)]

  /**
    * Each bin has limits lb <= x < ub
    *
    * @param thresholds input array of user-defined threshold values
    * @param prefix a string to use as a prefix symbol
    *
    * @return the function that discretizes
    */
  def mkSymbolic(thresholds: Array[Double], prefix: String): Double => String = {
    val sortedThresholds = thresholds.sorted

    (x: Double) =>
      val pos = binarySearch(sortedThresholds, x)
      val bin = if (pos < 0) -pos - 2 else pos
      prefix + bin
  }

  /**
    * Each bin has limits lb <= value.index < ub
    *
    * @param thresholds input array of user-defined threshold values
    *
    * @return the function that finds intervals from constants
    */
  def mkInterval(thresholds: Array[Double], symbols2domain: Map[String, String]): String => String = {
    val sortedThresholds = thresholds.sorted

    (x: String) =>
      val pos = x.last.toString.toInt
      val symbol = x.head.toString
      s"${symbols2domain(symbol)} >= ${sortedThresholds(pos)} and ${symbols2domain(symbol)} < ${sortedThresholds(pos + 1)}"
  }

  /**
    * Translates a time duration string value into seconds.
    *
    * @param duration a time duration string
    *
    * @return the time duration in seconds
    */
  def duration2Seconds(duration: String): Int = {
    val values = duration.split(":")
    values(0).toInt * 3600 + values(1).toInt * 60 + values(2).toInt
  }

  /**
    * Translates a date-time value into a unix time-stamp (in seconds) according to a
    * given date format for the date-times and a second short date-format in case some
    * date-time values are shorter (e.g. seconds are missing). Moreover, the unix
    * time-stamp can be shifted by a specified offset, translated into a time-stamp
    * relative to a given starting time-stamp and/or rounded to seconds (0, 15, 30, 45).
    *
    * @param dateTime a date-time string value
    * @param dateFormat a date format
    * @param dateFormatShort a short date format
    * @param offset an offset used to shift the produced time-stamp
    * @param startTs a starting time-stamp used to find the relative time-distance
    * @param round if true the date-time is rounded into seconds (0, 15, 30, 45)
    *
    * @return a unix time-stamp
    */
  def ts2UnixTS(dateTime: String, dateFormat: String, dateFormatShort: String,
                offset: Int = 0, startTs: Long = 0, round: Boolean = false) = {

    val validSeconds = Vector(0, 15, 30, 45)
    val components = dateTime.split(":")

    // Step 1. Round date to seconds 0, 15, 30 or 45
    val roundedDateTime =
      if (round && dateTime.trim.length == 19 && !validSeconds.contains(components.last.toInt)) {
        val replacement = validSeconds.minBy(v => math.abs(v - components.last.toInt))
        var result = StringBuilder.newBuilder
        for (i <- 0 until components.length - 1)
          result ++= components(i) + ":"
        result ++= replacement.toString
        result.result()
      }
      else dateTime

    // Step 2. Format date. In case not full date is available use short date format
    val simpleDateFormat =
      new SimpleDateFormat(
        if (roundedDateTime.trim.length == 19) dateFormat
        else dateFormatShort
      )

    // Step 3. Get unix time-stamp in seconds. If an offset is specified also shift the seconds by that offset
    val seconds = (simpleDateFormat.parse(roundedDateTime).getTime / 1000) + offset

    // Step 4. Return the result
    if (startTs > 0 && round)
      (startTs + ((seconds - startTs) / 15)).toInt
    else seconds.toInt
  }

}
