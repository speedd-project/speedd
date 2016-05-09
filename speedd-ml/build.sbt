import com.typesafe.sbt.packager.archetypes.JavaAppPackaging

name := "SPEEDD-ML"

version := "0.2"

scalaVersion := "2.11.8"

// Load SPEEDD ML Build settings
SPEEDDMLBuild.settings

enablePlugins(JavaAppPackaging)

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
  "ch.qos.logback" % "logback-classic" % "1.1.7",
  "org.slf4j" % "slf4j-api" % "1.7.21"
)

// Database
libraryDependencies ++= Seq(
  "com.typesafe.slick" %% "slick" % "3.1.1",
  "com.typesafe.slick" %% "slick-codegen" % "3.1.1"
)

// AuxLib library requires local publishing
libraryDependencies += "com.github.anskarl" %% "auxlib" % "0.1"

// Unit testing
libraryDependencies += "org.scalatest" %% "scalatest" % "2.2.4" % "test"

// Optimized Range foreach loops
libraryDependencies += "com.nativelibs4java" %% "scalaxy-streams" % "0.3.4" % "provided"

// lpsolve library for optimization
libraryDependencies += "com.datumbox" % "lpsolve" % "5.5.2.0"

// CSV parsing
libraryDependencies += "com.univocity" % "univocity-parsers" % "2.0.2"

// Dependency overrides
dependencyOverrides += "org.scala-lang" % "scala-compiler" % scalaVersion.value
dependencyOverrides += "org.scala-lang" % "scala-library" % scalaVersion.value
dependencyOverrides += "org.scala-lang" % "scala-reflect" % scalaVersion.value
dependencyOverrides += "org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.4"
dependencyOverrides += "org.scala-lang.modules" %% "scala-xml" % "1.0.4"


