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

package org.speedd.ml.app

import java.io.File
import java.nio.file.Paths
import java.util.regex.Pattern

import auxlib.log.Logging
import org.speedd.ml.ModuleVersion
import org.speedd.ml.util.IO._

import scala.util.{Success, Try}

trait CLIDataLoaderApp extends App with CommonOptions with Logging {

  println(s"${ModuleVersion()}\nData Loading Application")

  // -------------------------------------------------------------------------------------------------------------------
  // --- Configuration parameters
  // -------------------------------------------------------------------------------------------------------------------
  protected var filesFunc: (File, Boolean) => Try[Seq[File]] = (d: File, x: Boolean) => Success(Seq[File]())
  protected var rootDir = Paths.get(".")
  protected var recursion = false

  // -------------------------------------------------------------------------------------------------------------------
  // --- Command line interface options
  // -------------------------------------------------------------------------------------------------------------------
  opt("i", "input pattern", "<input pattern>",
  "regex: regular expression or suffix: file suffix or prefix: file prefix or files:comma separated file names. " +
    "For example, \"suffix:csv or regex:.*csv\" to find all csv files.", {

    v: String =>
      val patternType = v.substring(0, v.indexOf(':'))
      val patternDefinition = v.substring(v.indexOf(':') + 1, v.length)

      filesFunc = (targetDir: File, doRecursively: Boolean) =>
        patternType match {
          case "regex" =>
            val pattern = Pattern.compile(patternDefinition)
            findFiles(
              targetDir,
              matcherFunction = f => f.isFile && pattern.matcher(f.getName).matches(),
              recursively = doRecursively)

          case "prefix" =>
            findFiles(
              targetDir,
              matcherFunction = f => f.isFile && f.getName.startsWith(patternDefinition),
              recursively = doRecursively)

          case "suffix" =>
            println("SUFFIX: "+patternDefinition)
            findFiles(
              targetDir,
              matcherFunction = f => f.isFile && f.getName.endsWith(patternDefinition),
              recursively = doRecursively)

          case "file" =>
            Success(
              v.split(" ")
                .map(f => new File(f))
                .filter(f => f.exists && f.isFile))

          case _ => sys.error("Please define a valid input pattern.")
        }
  })

  opt("d", "directory", "<path>", "Specify the root directory to apply the transformation (Default is the current " +
    "working directory).", {
    v: String => rootDir = Paths.get(v)
  })

  flagOpt("r", "recursive", "Enable recursion, in order to search in sub-directories.", { recursion = true } )

}
