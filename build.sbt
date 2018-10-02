enablePlugins(GatlingPlugin)

scalaVersion := "2.12.7"

scalacOptions := Seq("-encoding",
                     "UTF-8",
                     "-target:jvm-1.8",
                     "-deprecation",
                     "-feature",
                     "-unchecked",
                     "-language:implicitConversions",
                     "-language:postfixOps")

val ammoniteVersion  = "1.2.1"
val circeVersion     = "0.10.0"
val commonsVersion   = "0.10.30"
val scalaTestVersion = "3.0.5"
val akkaVersion      = "2.5.17"
val serviceVersion   = "0.10.16"

lazy val commonsTest = nexusDep("commons-test", commonsVersion)
lazy val serviceHttp = nexusDep("service-http", serviceVersion)

lazy val perfTests = project
  .in(file("."))
  .enablePlugins(GatlingPlugin)
  .settings(libraryDependencies ++= Seq(
    "io.gatling.highcharts" % "gatling-charts-highcharts" % "3.0.0-RC3" % "test,it",
    "io.gatling"            % "gatling-test-framework"    % "3.0.0-RC3" % "test,it",
    commonsTest,
    serviceHttp,
    "com.lihaoyi"           %% "ammonite-ops" % ammoniteVersion,
    "io.circe"              %% "circe-core"   % circeVersion,
    "io.circe"              %% "circe-parser" % circeVersion,
    "io.circe"              %% "circe-parser" % circeVersion,
    "com.github.pureconfig" %% "pureconfig"   % "0.9.2",
    "org.scalatest"         %% "scalatest"    % scalaTestVersion % Test
  ))

def nexusDep(name: String, version: String): ModuleID = "ch.epfl.bluebrain.nexus" %% name % version
