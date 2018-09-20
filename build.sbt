enablePlugins(GatlingPlugin)

scalaVersion := "2.12.4"

scalacOptions := Seq("-encoding",
                     "UTF-8",
                     "-target:jvm-1.8",
                     "-deprecation",
                     "-feature",
                     "-unchecked",
                     "-language:implicitConversions",
                     "-language:postfixOps")

val ammoniteVersion  = "1.1.2"
val circeVersion     = "0.9.3"
val commonsVersion   = "0.10.22"
val scalaTestVersion = "3.0.5"
val akkaVersion      = "2.5.14"
val serviceVersion   = "0.10.15"

lazy val commonsTest = nexusDep("commons-test", commonsVersion)
lazy val serviceHttp = nexusDep("service-http", serviceVersion)


lazy val perfTests = project
  .in(file("."))
  .enablePlugins(GatlingPlugin)
  .settings(
    libraryDependencies ++= Seq(
      "io.gatling.highcharts" % "gatling-charts-highcharts" % "2.3.1" %  "test,it",
      "io.gatling"            % "gatling-test-framework"    % "2.3.1" %  "test,it",
      commonsTest,
      serviceHttp,
      "com.lihaoyi"   %% "ammonite-ops" % ammoniteVersion,
      "io.circe"      %% "circe-core"   % circeVersion,
      "io.circe"      %% "circe-parser" % circeVersion,
      "io.circe"      %% "circe-parser" % circeVersion,
      "org.scalatest" %% "scalatest"    % scalaTestVersion % Test
    ))


def nexusDep(name: String, version: String): ModuleID =
  "ch.epfl.bluebrain.nexus" %% name % version




