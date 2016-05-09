
import sbt._
import sbt.Keys._
import com.typesafe.sbt.packager.universal.UniversalPlugin.autoImport._

object SPEEDDMLBuild {

  val javaVersion = sys.props("java.specification.version").toDouble

  lazy val settings: Seq[Setting[_]] = {
    println(s"[info] Loading settings for Java $javaVersion or higher.")
    jdkSettings
  }

  private val commonSettings: Seq[Setting[_]] = Seq(

    scalaVersion := "2.11.8",
    autoScalaLibrary := true,
    managedScalaInstance := true,

    logLevel in Test := Level.Info,
    logLevel in Compile := Level.Error,

    javaOptions ++= Seq(
      "-XX:+DoEscapeAnalysis",
      "-XX:+UseFastAccessorMethods",
      "-XX:+OptimizeStringConcat",
      "-Dlogback.configurationFile=src/main/resources/logback.xml"),

    // fork a JVM for 'run' and 'test:run'
    fork := true,

    // fork a JVM for 'test:run', but not 'run'
    fork in Test := true,

    conflictManager := ConflictManager.latestRevision,

    // Include utility bash scripts in the 'bin' directory
    mappings in Universal <++= (packageBin in Compile) map { jar =>
      val scriptsDir = new java.io.File("scripts/")
      scriptsDir.listFiles.toSeq.map { f =>
        f -> ("bin/" + f.getName)
      }
    },

    // Include logger configuration file to the final distribution
    mappings in Universal <++= (packageBin in Compile) map { jar =>
      val scriptsDir = new java.io.File("src/main/resources/")
      scriptsDir.listFiles.toSeq.map { f =>
        f -> ("etc/" + f.getName)
      }
    }
  )

  private lazy val jdkSettings: Seq[Setting[_]] = commonSettings ++ Seq(
    javacOptions ++= Seq("-source", "1.8", "-target", "1.8", "-Xlint:unchecked", "-Xlint:deprecation"),
    scalacOptions ++= Seq(
      "-Yclosure-elim",
      "-Yinline",
      "-feature",
      "-target:jvm-1.8",
      "-language:implicitConversions",
      "-Ybackend:GenBCode"
    )
  )
}