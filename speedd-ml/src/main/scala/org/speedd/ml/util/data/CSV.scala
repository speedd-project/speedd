package org.speedd.ml.util.data

import java.io.File
import com.univocity.parsers.common.processor.RowListProcessor
import com.univocity.parsers.csv.{CsvParser, CsvParserSettings}

/**
  * A collection of various utility functions related to CSV parsing
  */
object CSV {

  /**
    * Parse an input CSV file and translate each line into an object T
    * given a translator functor.
    *
    * @param inputFile the input csv file
    * @param translator the translator functor
    * @tparam T the type of object
    *
    * @return a set of objects T
    */
  def parse[T](inputFile: File, translator: Array[String] => Option[T]): Set[T] = {

    val processor = new RowListProcessor()

    val settings = new CsvParserSettings()

    settings.setRowProcessor(processor)
    settings.getFormat.setDelimiter(',')
    settings.getFormat.setLineSeparator("\n")

    // CSV parser
    val parser = new CsvParser(settings)
    parser.parse(inputFile)

    val rows = processor.getRows

    var result = Set.empty[Option[T]]
    val iterator = rows.listIterator()
    while(iterator.hasNext)
      result += translator(iterator.next())

    result.flatten
  }

  /**
    * Create a CSV parse iterator given a CSV file.
    *
    * @param inputFile the input csv file
    *
    * @return a csv parser
    */
  def parseIterator(inputFile: File): CsvParser = {

    val settings = new CsvParserSettings()

    settings.getFormat.setDelimiter(',')
    settings.getFormat.setLineSeparator("\n")
    settings.setNullValue("null")

    // CSV parser
    val parser = new CsvParser(settings)

    // call beginParsing to read records one by one, iterator-style.
    parser.beginParsing(inputFile)

    parser
  }

  def parseNext[T](parser: CsvParser, translator: Array[String] => Option[T]): Option[T] = {
    val output = parser.parseNext()
    if (output != null)
      translator(output)
    else {
      parser.stopParsing()
      null
    }
  }

}

