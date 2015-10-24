/*
 *  __   ___   ____  ____  ___   ___
 * ( (` | |_) | |_  | |_  | | \ | | \
 * _)_) |_|   |_|__ |_|__ |_|_/ |_|_/
 *
 * SPEEDD project (www.speedd-project.eu)
 * Machine Learning module
 *
 * Copyright (c) Complex Event Recognition Group (cer.iit.demokritos.gr)
 *
 * NCSR Demokritos
 * Institute of Informatics and Telecommunications
 * Software and Knowledge Engineering Laboratory
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with program. If not, see <http://www.gnu.org/licenses/>.
 */

package org.speedd.ml

import java.io.File
import java.nio.file.{AccessDeniedException, NoSuchFileException, NotDirectoryException, Path}
import java.security.InvalidParameterException
import lomrf.logic.AtomSignature
import org.apache.spark.sql.Column
import org.apache.spark.sql.types._

import scala.language.implicitConversions
import scala.collection.mutable
import scala.util.{Success, Failure, Try}


package object util {

  /**
   * Implicitly convert the specified filepath as a string into the corresponding instance of file
   *
   * @param str the input filepath as a string
   *
   * @return the corresponding instance of file
   */
  implicit def strToFile(str: String): File = new File(str)

  /**
   * Implicitly convert a Path instance into the corresponding File instance
   *
   * @param path the input Path instance
   *
   * @return the corresponding instance of file
   */
  implicit def pathToFile(path: Path): File = path.toFile

  /**
   * Search recursively for files/directories in a directory.
   *
   * @param targetDir: the target directory to search
   * @param matcherFunction: a simple filtering function (maps files to Boolean values, where the value 'true'
   *                       represents that the file matches the filtering criteria of the function)

   * @param recursively When is set true the function searches recursively in all dirs. It is true by default.
   *
   * @return A `Success` of collected matched files (may empty if no matches found).  Otherwise it may fail, resulting
   *         to a `Failure`, due to: NoSuchFileException (when the target directory does not exists),
   *         or AccessDeniedException (when the application does not have read permissions to access),
   *         or NotDirectoryException (when the 'targetDir' parameter is not a directory).
   */
  def findFiles(targetDir: File, matcherFunction: File => Boolean, recursively: Boolean = true): Try[Seq[File]] = {

    onValidTargetDir (targetDir) {
      val directories = mutable.Queue[File](targetDir)

      var resultFiles = List[File]()

      while (directories.nonEmpty) {
        for (currentFile <- directories.dequeue().listFiles) {
          // When the current file is a directory and the recursively=true, then
          // simply enqueue this file in the directories queue, otherwise continue.
          if (recursively && currentFile.isDirectory) directories.enqueue(currentFile)

          // When the current file is matching (according to the given matcher function), then
          // add this file to the result list, otherwise continue.
          if (matcherFunction(currentFile)) resultFiles ::= currentFile
        }
      }

      Success(resultFiles)
    }

  }

  /**
   * Search recursively for files/directories in a directory and give the fist matching file.
   *
   * @param targetDir: the target directory to search
   * @param matcherFunction: a simple filtering function (maps files to Boolean values, where the value 'true'
   *                       represents that the file matches the filtering criteria of the function)

   * @param recursively When is set true the function searches recursively in all dirs. It is true by default.
   *
   * @return A `Success` containing an optional matched file.  Otherwise it may fail, resulting
   *         to a `Failure`, due to: NoSuchFileException (when the target directory does not exists),
   *         or AccessDeniedException (when the application does not have read permissions to access),
   *         or NotDirectoryException (when the 'targetDir' parameter is not a directory).
   */
  def findFirst(targetDir: File, matcherFunction: File => Boolean, recursively: Boolean = true): Try[Option[File]] = {

    onValidTargetDir (targetDir) {
      val directories = mutable.Queue[File](targetDir)


      while (directories.nonEmpty) {
        for (currentFile <- directories.dequeue().listFiles) {
          // When the current file is a directory and the recursively=true, then
          // simply enqueue this file in the directories queue, otherwise continue.
          if (recursively && currentFile.isDirectory) directories.enqueue(currentFile)

          // When the current file is matching (according to the given matcher function), then
          // add this file to the result list, otherwise continue.
          if (matcherFunction(currentFile)) return Success(Some(currentFile))
        }
      }

      Success(None)
    }

  }

  private def onValidTargetDir[T](targetDir: File)(body: => Try[T]): Try[T] ={
    if (!targetDir.exists())
      return Failure(new NoSuchFileException("The specified target does not exist"))

    if (!targetDir.isDirectory)
      return Failure(new NotDirectoryException("The specified target does not seem to be a directory"))

    if (!targetDir.canRead)
      return Failure(new AccessDeniedException("Cannot read the specified target, please check if you have sufficient permissions."))

    body
  }


  object csv {

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
     * Defines six coliumes, identifed by the names: `loc`, `lane`, `prev_lane`, `coordX`, `cooedY` and `num`.
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
     *   byte and short.
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

      def parseAtomic(atomicDef: String) ={
        atomicDef match{
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
              val elementType = parseAtomic(columnTypeStr.substring(6, columnTypeStr.length-1))
              ArrayType(elementType)

            case r"map<[a-zA-Z]+/[a-zA-Z]+>" =>
              val kvTypeDef = columnTypeStr.substring(4,columnTypeStr.length-1).split("/")
              val keyType = parseAtomic(kvTypeDef(0).trim)
              val valueType = parseAtomic(kvTypeDef(1).trim)
              MapType(keyType,valueType)

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

    implicit class WrappedStructType(val src: StructType) extends AnyVal{
      def toColumns: Array[Column] ={
        src.fields.map(field => new Column(field.name))
      }
    }

    implicit class Regex(sc: StringContext) {
      def r = new scala.util.matching.Regex(sc.parts.mkString, sc.parts.tail.map(_ => "x"): _*)
    }

  }

  /**
   * Parses the given string, which is expected to contain atomic signatures that are separated by commas. For example,
   * consider the following string:
   *
   * {{{
   *   parseSignatures(src = "HoldsAt/2,InitiatedAt/2, TerminatedAt/2")
   * }}}
   *
   * For the above string this function will produce the following successful result:
   *
   * {{{
   *   Success( Set( AtomSignature("HoldsAt", 2), AtomSignature("InitiatedAt", 2), AtomSignature("TerminatedAt", 2) ) )
   * }}}
   *
   * In situations where the given string is not following the expected format, this function will give a Failure with
   * the caused exception.
   *
   * @param src source string composed of comma separated atomic signatures
   *
   * @return a Success try containing a collection of AtomSignatures from the specified source string, otherwise a
   *         Failure containing the caused exception.
   */
  def parseSignatures(src: String): Try[Set[AtomSignature]] = Try {
    src.split(",").map{ entry =>
      val (symbol, arity) = entry.span(_ == '/')
      AtomSignature(symbol.trim, arity.trim.toInt)
    }(scala.collection.breakOut)
  }

}
