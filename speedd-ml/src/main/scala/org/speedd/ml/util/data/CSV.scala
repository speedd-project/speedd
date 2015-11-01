package org.speedd.ml.util.data

import java.security.InvalidParameterException

import org.apache.spark.sql.Column
import org.apache.spark.sql.types._
import scala.language.implicitConversions

/**
 * A collection of various utility functions related to CSV parsing
 */
object CSV {

  val CSV_FORMAT = "com.databricks.spark.csv"

  val CSV_OPTIONS = Map(
    "delimiter" -> ",", // use standard column delimiter
    "header" -> "false", // don't expect that the input file will have a header information
    "parserLib" -> "UNIVOCITY", // use UNIVOCITY CSV parser back-end for memory and CPU efficiency
    "mode" -> "DROPMALFORMED" // ignore lines with errors
  )


  /**
   * Creates a CSV schema definition from the given string. For example, the following line:
   *
   * {{{
   *   loc, lane, ?prev_lane, coordX:double, coordY:double, num:int
   * }}}
   *
   * Defines six columns, identified by the names: `loc`, `lane`, `prev_lane`, `coordX`, `cooedY` and `num`.
   * The entries of columns `loc`, `lane` and `pev_lane`, take string elements (implicitly defined).
   * The entries of columns `coordX` and `coordY` take floating point numbers of double precision, similarly, the
   * entries of column `num` accept integer numbers. Finally, the symbol `?` defines that the column `prev_lane`
   * can also accept null values. Therefore, the other columns do not accept null values.
   *
   * Following the example, the schema of each column can be defined with the following notation:
   * {{{
   *   [?] name [:type]
   * }}}
   * - The `?` is optional and defines whether the column accepts null values
   * - The `name` uniquely defines the column name and is a required field.
   * - `:type` defines the type of the entries, i.e., int (or integer), double, bool (boolean), float, long,
   * byte and short.
   *
   *
   * @param src the specified CSV schema definition
   *
   * @throws InvalidParameterException when an unknown column type is given
   *
   * @return The resulting `StructType` of the specified schema.
   *
   * @see [[org.apache.spark.sql.types.StructType]]
   */
  @throws[InvalidParameterException]
  def parseSchema(src: String): StructType = {

    def parseAtomic(atomicDef: String) = {
      atomicDef match {
        case "int" | "integer" => IntegerType
        case "double" => DoubleType
        case "bool" | "boolean" => BooleanType
        case "float" => FloatType
        case "long" => LongType
        case "byte" => ByteType
        case "short" => ShortType
        case "string" => StringType
        case unknownType =>
          throw new InvalidParameterException(s"Unknown type '$unknownType'")
      }
    }

    val fields = src.split(",") map { name =>

      val trimmedName = name.trim
      val isNullable = trimmedName.charAt(0) == '?'
      val entry = trimmedName.split(":")

      if (entry.length == 2) {

        val columnName = (if (isNullable) entry(0).substring(1) else entry(0)).trim()
        val columnTypeStr = entry(1).trim.toLowerCase

        val columnType = columnTypeStr match {
          case r"array<[a-zA-Z]+>" =>
            val elementType = parseAtomic(columnTypeStr.substring(6, columnTypeStr.length - 1))
            ArrayType(elementType)

          case r"map<[a-zA-Z]+/[a-zA-Z]+>" =>
            val kvTypeDef = columnTypeStr.substring(4, columnTypeStr.length - 1).split("/")
            val keyType = parseAtomic(kvTypeDef(0).trim)
            val valueType = parseAtomic(kvTypeDef(1).trim)
            MapType(keyType, valueType)

          case atomicType: String => parseAtomic(atomicType)
        }

        StructField(columnName, columnType, nullable = isNullable)

      } else {
        val entryName = if (isNullable) trimmedName.substring(1) else trimmedName
        StructField(entryName, StringType, nullable = isNullable)
      }
    }

    StructType(fields)
  }

  implicit class WrappedStructType(val src: StructType) extends AnyVal {
    def toColumns: Array[Column] = {
      src.fields.map(field => new Column(field.name))
    }
  }

  implicit class Regex(sc: StringContext) {
    def r = new scala.util.matching.Regex(sc.parts.mkString, sc.parts.tail.map(_ => "x"): _*)
  }

}