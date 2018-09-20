package ch.epfl.bluebrain.nexus

import java.net.URLEncoder

import akka.http.scaladsl.model.Uri
import ch.epfl.bluebrain.nexus.data.generation.ResourcesGenerator
import ch.epfl.bluebrain.nexus.data.generation.types.Settings
import io.gatling.core.Predef._
import io.gatling.http.Predef._

import scala.concurrent.duration._

class UploadSimulation extends Simulation {

  val journeyDuration = 1 minutes

  //TODO read from config
  val kgBase = "http://localhost:8080/v1"
  val token =
    "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJvY1NGaDhublE3ZHhFYllDN3ZuUnBvWGF0RW9ub2wwQVRROVRhS0MtSHVRIn0.eyJqdGkiOiIxY2VlNjkzOC01NDBmLTQ3NDMtOWRlNi1lNmRlZDRkZDBlMDgiLCJleHAiOjE5NjczNzk4NDgsIm5iZiI6MCwiaWF0IjoxNTM1Mzc5ODQ4LCJpc3MiOiJodHRwOi8vZGV2Lm5leHVzLm9jcC5iYnAuZXBmbC5jaC9hdXRoL3JlYWxtcy9iYnAtdGVzdCIsImF1ZCI6Im5leHVzLWRldiIsInN1YiI6IjNlZWQyNzI3LTU1NTYtNDEyOS1iNzJjLTY4Mjg3NDAyNDZhYSIsInR5cCI6IkJlYXJlciIsImF6cCI6Im5leHVzLWRldiIsImF1dGhfdGltZSI6MTUzNTM3OTg0OCwic2Vzc2lvbl9zdGF0ZSI6ImYxMThlODUzLTg5NDYtNGU1OS1iMDdkLTdlNzljNjJhMzUzZiIsImFjciI6IjEiLCJhbGxvd2VkLW9yaWdpbnMiOltdLCJyZWFsbV9hY2Nlc3MiOnsicm9sZXMiOlsidW1hX2F1dGhvcml6YXRpb24iXX0sInJlc291cmNlX2FjY2VzcyI6eyJhY2NvdW50Ijp7InJvbGVzIjpbIm1hbmFnZS1hY2NvdW50IiwibWFuYWdlLWFjY291bnQtbGlua3MiLCJ2aWV3LXByb2ZpbGUiXX19LCJuYW1lIjoiV29qY2llY2ggV2FqZXJvd2ljeiIsInByZWZlcnJlZF91c2VybmFtZSI6Ind3YWplcm93aWN6IiwiZ2l2ZW5fbmFtZSI6IldvamNpZWNoIiwiZmFtaWx5X25hbWUiOiJXYWplcm93aWN6IiwiZW1haWwiOiJ3b2pjaWVjaC53YWplcm93aWN6QGVwZmwuY2gifQ.RgIZvl3R7aVDecmAJkx6SY4ayki7hXbtjRgk12lyFpwV3GnAwZHndeEQi1SyaOUWr5K1tLwPLXdCuuZQs5TP6jeUV2O7DJ1hwDuSNTZoPwfDs9Ys5kIkazX0iho-xDYKYly4lXj_jsaFS8aVRyIWUpQcj77JSd_V9rOUMzRW7kn5l8AhgWi2eYKHMQrV4GgaecUT96Dy9LHnfUEdOOxXAQ4dxDUZPKs_qvY7ZEdsp1djubCBAU5wGLMXCQb-9ORNpUsTvc3O7qbHzLV2duRuEEluKqDr6DuKE6irtMHUK4leviKZhmjw-DLA4mCV8POhOZ3Ch0zE0Qig2QsL7gocOQ"
  val numProjects    = 2
  val parallelUsers = 2

  val map = Map[String, Uri](
    "person"                -> "https://bluebrain.github.io/nexus/schemas/neurosciencegraph/core/person",
    "stimulusexperiment"    -> "https://bluebrain.github.io/nexus/schemas/neurosciencegraph/stimulusexperiment",
    "trace"                 -> "https://bluebrain.github.io/nexus/schemas/neurosciencegraph/trace",
    "tracegeneration"       -> "https://bluebrain.github.io/nexus/schemas/neurosciencegraph/tracegeneration",
    "brainslicing"          -> "https://bluebrain.github.io/nexus/schemas/experiment/brainslicing",
    "patchedcell"           -> "https://bluebrain.github.io/nexus/schemas/experiment/patchedcell",
    "patchedcellcollection" -> "https://bluebrain.github.io/nexus/schemas/experiment/patchedcellcollection",
    "patchedslice"          -> "https://bluebrain.github.io/nexus/schemas/experiment/patchedcellslice",
    "protocol"              -> "https://bluebrain.github.io/nexus/schemas/neurosciencegraph/core/protocol",
    "slice"                 -> "https://bluebrain.github.io/nexus/schemas/neurosciencegraph/core/slice",
    "subject"               -> "https://bluebrain.github.io/nexus/schemas/neurosciencegraph/core/subject",
    "wholecellpatchclamp"   -> "https://bluebrain.github.io/nexus/schemas/experiment/wholecellpatchclamp"
  )
  val settings: Settings = Settings(Uri("http://example.com/ids/"), map)
  val data =
    (1 to numProjects).flatMap{i =>
      ResourcesGenerator(1, Math.pow(10, i).toInt, 20)(settings).right.get.map((i, _))}

  val feeder = data
    .map {
      case (project, instance) =>
        println(instance.id)
        Map(
          "payload"          -> instance.payload,
          "project"          -> project,
          "schema"           -> URLEncoder.encode(instance.schema.toString, "UTF-8"),
          "schemaNonEncoded" -> instance.schema.toString
        )
    }
    .toArray
    .queue

  val httpConf = http
    .baseURL(kgBase) // Here is the root for all relative URLs
    .authorizationHeader(s"Bearer $token")

  val scn = scenario("fetching data")
    .repeat(data.size / parallelUsers) {
      feed(feeder)
        .exec(
          http("post to ${schemaNonEncoded}")
            .post("/resources/perftestorg/perftestproj${project}/${schema}")
            .body(StringBody("${payload}"))
            .header("Content-Type", "application/json")
            .check(status.in(201, 409))
        )
    }

//  setUp(scn.inject(constantUsersPerSec(0.5) during (10 seconds)).protocols(httpConf))
  setUp(scn.inject(atOnceUsers(parallelUsers)).protocols(httpConf))
}
