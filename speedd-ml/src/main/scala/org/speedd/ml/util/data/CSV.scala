package org.speedd.ml.util.data

import java.io.{File, FileInputStream, InputStream, UnsupportedEncodingException}
import java.util.zip.{ZipInputStream, GZIPInputStream}
import com.univocity.parsers.common.processor.RowListProcessor
import com.univocity.parsers.csv.{CsvParser, CsvParserSettings}
import scala.util.{Failure, Success, Try}

/**
  * A collection of various utility functions related to CSV parsing
  */
object CSV {

  /**
    * Maps a given file into an `InputStream` according to its extension. It supports
    * .gz, .zip and .csv file extensions.
    *
    * @param inputFile a given file
    * @return an input stream
    */
  private def toInputStream(inputFile: File): Try[InputStream] = inputFile.getName match {

    case fileName if fileName.matches(".*[.]gz") =>
      Success(new GZIPInputStream(new FileInputStream(inputFile)))

    case fileName if fileName.matches(".*[.]zip") =>
      Success(new ZipInputStream(new FileInputStream(inputFile)))

    case fileName if fileName.matches(".*[.]csv") =>
      Success(new FileInputStream(inputFile))

    case _ => Failure(new UnsupportedEncodingException(s"Unsupported extension for file ${inputFile.getName}!"))
  }

  /**
    * Parse an input file and translate each line into an object T
    * given a translator functor.
    *
    * @param inputFile the input file
    * @param translator the translator functor
    * @param skipHeader skip the first line of the csv (default is false)
    * @tparam T the type of object
    *
    * @return a set of objects T
    */
  def parse[T](inputFile: File, translator: Array[String] => Option[T], skipHeader: Boolean = false): Try[Set[T]] = {

    val inputStream: InputStream = toInputStream(inputFile) match {
      case Success(stream) => stream
      case Failure(ex) => return Failure(ex)
    }

    val processor = new RowListProcessor()

    val settings = new CsvParserSettings()

    settings.setRowProcessor(processor)
    settings.getFormat.setDelimiter(',')
    settings.getFormat.setLineSeparator("\n")

    if (skipHeader)
      settings.setNumberOfRowsToSkip(1)

    // CSV parser
    val parser = new CsvParser(settings)
    parser.parse(inputStream)

    val rows = processor.getRows

    var result = Set.empty[Option[T]]
    val iterator = rows.listIterator()
    while(iterator.hasNext)
      result += translator(iterator.next())

    Success(result.flatten)
  }

  /**
    * Create a CSV parse iterator given a file.
    *
    * @param inputFile the input csv file
    * @param skipHeader skip the first line of the csv (default is false)
    *
    * @return a csv parser
    */
  def parseIterator(inputFile: File, skipHeader: Boolean = false): Try[CsvParser] = {

    val inputStream: InputStream = toInputStream(inputFile) match {
      case Success(stream) => stream
      case Failure(ex) => return Failure(ex)
    }

    val settings = new CsvParserSettings()

    settings.getFormat.setDelimiter(',')
    settings.getFormat.setLineSeparator("\n")
    settings.setNullValue("null")

    if (skipHeader)
      settings.setNumberOfRowsToSkip(1)

    // CSV parser
    val parser = new CsvParser(settings)

    // call beginParsing to read records one by one, iterator-style.
    parser.beginParsing(inputStream)

    Success(parser)
  }

  /**
    * Parses the next record and returns a result if any exist or a failure if
    * the end of file is reached.
    *
    * @param parser a csv parser
    * @param translator a translator function that maps an array of strings into an object
    * @param externalFields add external optional fields in the parsed csv line (default is None)
    * @tparam T a type of object to parse
    *
    * @return a parsed object T
    */
  def parseNext[T](parser: CsvParser, translator: Array[String] => Option[T], externalFields: Option[Array[String]] = None): Try[Option[T]] = {

    val output = parser.parseNext()

    if (output != null && externalFields.isDefined) Success(translator(output ++ externalFields.get))
    else if (output != null) Success(translator(output))
    else {
      parser.stopParsing()
      Failure(new NullPointerException)
    }
  }

  /**
    * Parses the next batch of records and returns a list of results if any exist or a failure if
    * the end of file is reached.
    *
    * @param parser a csv parser
    * @param translator a translator function that maps an array of strings into an object
    * @param batchSize the batch size (default is 1000)
    * @param externalFields add external optional fields in the parsed csv line (default is None)
    * @tparam T a type of object to parse
    *
    * @return a parsed batch of objects T
    */
  def parseNextBatch[T](parser: CsvParser, translator: Array[String] => Option[T],
                        batchSize: Int = 1000, externalFields: Option[Array[String]] = None): Try[List[T]] = {

    var records = List[T]()

    if (!parser.getContext.isStopped) for (i <- 1 to batchSize) {
      parseNext[T](parser, translator, externalFields) match {
        case Success(Some(result)) => records :+= result
        case Failure(ex) if records.nonEmpty => Success(records)
        case Failure(ex) => Failure(new NullPointerException)
      }
    }
    else
      return Failure(new NullPointerException)

    Success(records)
  }

}

