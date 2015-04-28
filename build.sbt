name := """Alimonia"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

// Adding Sonatype snapshots for experimental tools (Obey, TQL) to resolvers.
resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"

scalaVersion := "2.11.6"

libraryDependencies ++= Seq(

  // Play dependencies
  jdbc,
  anorm,
  cache,
  ws,
  filters,

  // library dependencies
  "oauth.signpost" % "signpost-core" % "1.2.1.2",
  "oauth.signpost" % "signpost-commonshttp4" % "1.2.1.1",
  "org.apache.httpcomponents" % "httpclient" % "4.3.5",
  "org.mockito" % "mockito-core" % "1.9.5",
  "commons-io" % "commons-io" % "2.4",

  // Database dependencies
  "postgresql" % "postgresql" % "9.1-901-1.jdbc4"
)

scalacOptions += "-feature"

parallelExecution in Test := true
