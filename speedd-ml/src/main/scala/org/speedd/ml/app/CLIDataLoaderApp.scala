package org.speedd.ml.app

import java.io.File
import java.nio.file.Paths
import java.util.regex.Pattern

import org.speedd.ml.ModuleVersion
import org.speedd.ml.util.IO._

import scala.util.{Success, Try}
import auxlib.log.Logging
import auxlib.opt.OptionParser

/**
  * Data loading application interface. Should be implemented by all command line applications
  * intended for loading data into a database.
  */
trait CLIDataLoaderApp extends App with OptionParser with Logging {

  info(s"${ModuleVersion()}\nData Loading Application")

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
    "regex: regular expression | suffix: file suffix | prefix: file prefix | files: comma separated file names.\n" +
      "Example: \"suffix:csv or regex:.*csv\" to find all csv files.", {

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

  opt("d", "directory", "<path>", "Specify the root directory containing the data. Default is the current " +
    "working directory.", {
    v: String => rootDir = Paths.get(v)
  })

  flagOpt("r", "recursive", "Enable recursion, in order to search in sub-directories.", { recursion = true } )
}
