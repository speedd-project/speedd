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

package org.speedd.ml.util

import java.io.File
import java.nio.file.{AccessDeniedException, NotDirectoryException, NoSuchFileException, Path}
import scala.collection.mutable
import scala.util.{Failure, Success, Try}
import scala.language.implicitConversions

/**
 * A collection of various IO functions
 */
object IO {

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

    onValidTargetDir(targetDir) {
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

    onValidTargetDir(targetDir) {
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

  private def onValidTargetDir[T](targetDir: File)(body: => Try[T]): Try[T] = {
    if (!targetDir.exists())
      return Failure(new NoSuchFileException("The specified target does not exist"))

    if (!targetDir.isDirectory)
      return Failure(new NotDirectoryException("The specified target does not seem to be a directory"))

    if (!targetDir.canRead)
      return Failure(new AccessDeniedException("Cannot read the specified target, please check if you have sufficient permissions."))

    body
  }

}
