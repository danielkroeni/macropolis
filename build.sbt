name := "macropolis"

version := "1.0"

scalaVersion := "2.10.0-M7"

libraryDependencies <+= (scalaVersion)("org.scala-lang" % "scala-compiler" % _)

libraryDependencies <+= (scalaVersion)("org.scala-lang" % "scala-reflect" % _)

