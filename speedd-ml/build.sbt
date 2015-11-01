import sbt.Keys._
import sbt._

name := "SPEEDD-ML"

version := "0.1"

organization := "NCSR_Demokritos"

scalaVersion := "2.11.7"

autoScalaLibrary := true

managedScalaInstance := true

initialize := {
  initialize.value
  val javaVersion = sys.props("java.specification.version").toDouble
  if (javaVersion < 1.7) {
    sys.error("Java 7 or higher is required for this project")
    sys.exit(1)
  }
  if(javaVersion >= 1.8 ){
    println("[info] Loading settings for Java 8 or higher")
    javacOptions ++= Seq("-source", "1.8", "-target", "1.8", "-Xlint:unchecked", "-Xlint:deprecation")

    javaOptions ++= Seq(
      "-XX:+DoEscapeAnalysis",
      "-XX:+UseFastAccessorMethods",
      "-XX:+OptimizeStringConcat",
      "-Dlogback.configurationFile=src/main/resources/logback.xml")

    scalacOptions ++= Seq(
      "-Yclosure-elim",
      "-Yinline",
      "-feature",
      "-target:jvm-1.8",
      "-language:implicitConversions",
      "-Ybackend:GenBCode" //use the new optimisation level
    )
  } else {
    println("[info] Loading settings for Java 7")
    javacOptions ++= Seq("-source", "1.7", "-target", "1.7", "-Xlint:unchecked", "-Xlint:deprecation")

    scalacOptions ++= Seq(
      "-Yclosure-elim",
      "-Yinline",
      "-feature",
      "-target:jvm-1.7",
      "-language:implicitConversions",
      "-optimize" // old optimisation level
    )
  }
}

enablePlugins(JavaAppPackaging)

logLevel in Test := Level.Info
logLevel in Compile := Level.Error


// Add JVM options to use when forking a JVM for 'run'
javaOptions ++= Seq(
  "-XX:+DoEscapeAnalysis",
  "-XX:+UseFastAccessorMethods",
  "-XX:+OptimizeStringConcat",
  "-Dlogback.configurationFile=src/main/resources/logback.xml")


// fork a new JVM for 'run' and 'test:run'
fork := true

// fork a new JVM for 'test:run', but not 'run'
fork in Test := true

conflictManager := ConflictManager.latestRevision

/** Dependencies */
resolvers ++= Seq(
	"typesafe" at "http://repo.typesafe.com/typesafe/releases/",
	"sonatype-oss-public" at "https://oss.sonatype.org/content/groups/public/")

resolvers += Resolver.sonatypeRepo("snapshots")
	
// Scala-lang
libraryDependencies ++= Seq(
  "org.scala-lang" % "scala-library" % scalaVersion.value,
  "org.scala-lang" % "scala-reflect" % scalaVersion.value
)


// Scala-modules
libraryDependencies += "org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.3"

// Logging with slf4j and logback
libraryDependencies ++= Seq(
	"ch.qos.logback" % "logback-classic" % "1.1.3",
	"org.slf4j" % "slf4j-api" % "1.7.12"
)

// CSV parsing
libraryDependencies += "com.univocity" % "univocity-parsers" % "1.5.5"

// Logical Markov Random Fields (for details see https://github.com/anskarl/LoMRF)
libraryDependencies += "com.github.anskarl" %% "lomrf" % "0.4.2"

// Unit testing
libraryDependencies += "org.scalatest" %% "scalatest" % "2.2.4" % "test"

// Optimized Range foreach loops
libraryDependencies += "com.nativelibs4java" %% "scalaxy-streams" % "0.3.4" % "provided"

// Adding auxlib library requires local publishing (for details see https://github.com/anskarl/auxlib)
libraryDependencies += "com.github.anskarl" %% "auxlib" % "0.1"

// Apache Spark
libraryDependencies += "org.apache.spark" %% "spark-core" % "1.5.1" % "provided" exclude("org.slf4j", "slf4j-log4j12")
libraryDependencies += "org.apache.spark" %% "spark-sql" % "1.5.1"  % "provided" exclude("org.slf4j", "slf4j-log4j12")

libraryDependencies += "com.databricks" %% "spark-csv" % "1.2.0"

libraryDependencies += "com.datastax.spark" %% "spark-cassandra-connector" % "1.5.0-M2"
libraryDependencies += "org.javassist" % "javassist" % "3.20.0-GA"


// lpsolve library for optimization
libraryDependencies += "com.datumbox" % "lpsolve" % "5.5.2.0"

// Dependency overrides
dependencyOverrides += "org.scala-lang" % "scala-compiler" % scalaVersion.value
dependencyOverrides += "org.scala-lang" % "scala-library" % scalaVersion.value
dependencyOverrides += "org.scala-lang" % "scala-reflect" % scalaVersion.value
dependencyOverrides += "org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.4"
dependencyOverrides += "org.scala-lang.modules" %% "scala-xml" % "1.0.4"
dependencyOverrides += "jline" % "jline" % "2.12.1"

dependencyOverrides += "com.univocity" % "univocity-parsers" % "1.5.5"
dependencyOverrides += "ch.qos.logback" % "logback-classic" % "1.1.3"
dependencyOverrides += "org.slf4j" % "slf4j-api" % "1.7.12"

dependencyOverrides += "io.netty" % "netty" % "3.9.0.Final"

// Force Akka to version 2.3.11 (=the version that Spark 1.5.1 uses)
dependencyOverrides += "com.typesafe.akka" %% "akka-actor"  % "2.3.11"
dependencyOverrides += "com.typesafe.akka" %% "akka-remote"  % "2.3.11"
dependencyOverrides += "com.typesafe.akka" %% "akka-slf4j"  % "2.3.11"

// Merge strategy
assemblyMergeStrategy in assembly := {
  case "application.conf" => MergeStrategy.rename
  case "logback.xml" => MergeStrategy.rename
  case "logback-debug.xml" => MergeStrategy.rename
  case "logj4.properties" => MergeStrategy.rename
  case "META-INF/MANIFEST.MF" => MergeStrategy.discard
  case _ => MergeStrategy.first
}

assemblyJarName in assembly := "NCSR_SPEEDD-ML.jar"


// Removes all jar mappings in universal and appends the fat jar
// Fat jar is required from spark-submit tool
mappings in Universal := {
  // universalMappings: Seq[(File,String)]
  val universalMappings = (mappings in Universal).value
  val fatJar = (assembly in Compile).value
  // removing means filtering
  val filtered = universalMappings filter {
    case (file, fileName) =>  ! fileName.endsWith(".jar")
  }

  // add the fat jar
  filtered :+ (fatJar -> ("lib/" + fatJar.getName))
}


// Include utility bash scripts in the 'bin' directory
mappings in Universal <++= (packageBin in Compile) map { jar =>
  val scriptsDir = new java.io.File("scripts/")
  scriptsDir.listFiles.toSeq.map { f =>
    f -> ("bin/" + f.getName)
  }
}

// Include logger configuration file to the final distribution
mappings in Universal <++= (packageBin in Compile) map { jar =>
  val scriptsDir = new java.io.File("src/main/resources/")
  scriptsDir.listFiles.toSeq.map { f =>
    f -> ("etc/" + f.getName)
  }
}