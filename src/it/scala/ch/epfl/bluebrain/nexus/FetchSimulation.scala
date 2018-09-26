package ch.epfl.bluebrain.nexus

import java.net.URLEncoder
import java.util.UUID
import java.util.concurrent.ThreadLocalRandom

import io.circe.{Json, JsonObject}
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.circe.parser.parse

import scala.concurrent.duration._

class FetchSimulation extends Simulation {

  val nonUpdateableFields =
    Set("@id", "@type", "@context", "startedAtTime", "datePublished", "retrievalDate", "dataUnit")

  val kgBase = "http://localhost:8080/v1"
  val token =
    "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJvY1NGaDhublE3ZHhFYllDN3ZuUnBvWGF0RW9ub2wwQVRROVRhS0MtSHVRIn0.eyJqdGkiOiIxY2VlNjkzOC01NDBmLTQ3NDMtOWRlNi1lNmRlZDRkZDBlMDgiLCJleHAiOjE5NjczNzk4NDgsIm5iZiI6MCwiaWF0IjoxNTM1Mzc5ODQ4LCJpc3MiOiJodHRwOi8vZGV2Lm5leHVzLm9jcC5iYnAuZXBmbC5jaC9hdXRoL3JlYWxtcy9iYnAtdGVzdCIsImF1ZCI6Im5leHVzLWRldiIsInN1YiI6IjNlZWQyNzI3LTU1NTYtNDEyOS1iNzJjLTY4Mjg3NDAyNDZhYSIsInR5cCI6IkJlYXJlciIsImF6cCI6Im5leHVzLWRldiIsImF1dGhfdGltZSI6MTUzNTM3OTg0OCwic2Vzc2lvbl9zdGF0ZSI6ImYxMThlODUzLTg5NDYtNGU1OS1iMDdkLTdlNzljNjJhMzUzZiIsImFjciI6IjEiLCJhbGxvd2VkLW9yaWdpbnMiOltdLCJyZWFsbV9hY2Nlc3MiOnsicm9sZXMiOlsidW1hX2F1dGhvcml6YXRpb24iXX0sInJlc291cmNlX2FjY2VzcyI6eyJhY2NvdW50Ijp7InJvbGVzIjpbIm1hbmFnZS1hY2NvdW50IiwibWFuYWdlLWFjY291bnQtbGlua3MiLCJ2aWV3LXByb2ZpbGUiXX19LCJuYW1lIjoiV29qY2llY2ggV2FqZXJvd2ljeiIsInByZWZlcnJlZF91c2VybmFtZSI6Ind3YWplcm93aWN6IiwiZ2l2ZW5fbmFtZSI6IldvamNpZWNoIiwiZmFtaWx5X25hbWUiOiJXYWplcm93aWN6IiwiZW1haWwiOiJ3b2pjaWVjaC53YWplcm93aWN6QGVwZmwuY2gifQ.RgIZvl3R7aVDecmAJkx6SY4ayki7hXbtjRgk12lyFpwV3GnAwZHndeEQi1SyaOUWr5K1tLwPLXdCuuZQs5TP6jeUV2O7DJ1hwDuSNTZoPwfDs9Ys5kIkazX0iho-xDYKYly4lXj_jsaFS8aVRyIWUpQcj77JSd_V9rOUMzRW7kn5l8AhgWi2eYKHMQrV4GgaecUT96Dy9LHnfUEdOOxXAQ4dxDUZPKs_qvY7ZEdsp1djubCBAU5wGLMXCQb-9ORNpUsTvc3O7qbHzLV2duRuEEluKqDr6DuKE6irtMHUK4leviKZhmjw-DLA4mCV8POhOZ3Ch0zE0Qig2QsL7gocOQ"
  val project = 1

  val reads  = 10
  val writes = 1

  val schemas = List(
    Map("schema" -> "https://bluebrain.github.io/nexus/schemas/experiment/wholecellpatchclamp"),
    Map("schema" -> "https://bluebrain.github.io/nexus/schemas/neurosciencegraph/core/subject"),
    Map("schema" -> "https://bluebrain.github.io/nexus/schemas/neurosciencegraph/core/slice"),
    Map("schema" -> "https://bluebrain.github.io/nexus/schemas/neurosciencegraph/core/protocol"),
    Map("schema" -> "https://bluebrain.github.io/nexus/schemas/experiment/patchedcellslice"),
    Map("schema" -> "https://bluebrain.github.io/nexus/schemas/experiment/patchedcellcollection"),
    Map("schema" -> "https://bluebrain.github.io/nexus/schemas/experiment/patchedcell"),
    Map("schema" -> "https://bluebrain.github.io/nexus/schemas/experiment/brainslicing"),
    Map("schema" -> "https://bluebrain.github.io/nexus/schemas/neurosciencegraph/tracegeneration"),
    Map("schema" -> "https://bluebrain.github.io/nexus/schemas/neurosciencegraph/trace"),
    Map("schema" -> "https://bluebrain.github.io/nexus/schemas/neurosciencegraph/stimulusexperiment"),
    Map("schema" -> "https://bluebrain.github.io/nexus/schemas/neurosciencegraph/core/person")
  ).toArray.circular

  val schemaFeeder = ""

//  val journeyDuration = 5 minutes
  val journeyDuration = 10 minutes
  val httpConf = http
    .baseURL(kgBase) // Here is the root for all relative URLs
    .authorizationHeader(s"Bearer $token")

  val scn = scenario("fetching data")
    .feed(schemas)
    .exec { session =>
      val s = session("schema").as[String]
      session.set("encodedSchema", URLEncoder.encode(s, "UTF-8"))
    }
    .exec(
      http("list ${schema}")
        .get(s"/resources/perftestorg/perftestproj$project/$${encodedSchema}")
        check (jsonPath("$.._total")
          .ofType[Int]
          .saveAs("search_total")))
    .during(journeyDuration)(
      repeat(reads)(
        exec { session =>
          val rnd = ThreadLocalRandom
            .current()
            .nextInt(session("search_total").as[Int]) + 1
          val s = session("schema").as[String]
          session.set("encodedId", URLEncoder.encode(s"$s/ids/$rnd", "UTF-8"))

        }.exec(
          http("fetch from ${schema}")
            .get(s"/resources/perftestorg/perftestproj$project/$${encodedSchema}/$${encodedId}")
        )
      ).repeat(writes)(
        exec { session =>
          val rnd = ThreadLocalRandom
            .current()
            .nextInt(session("search_total").as[Int]) + 1
          val s = session("schema").as[String]
          session.set("encodedId", URLEncoder.encode(s"$s/ids/$rnd", "UTF-8"))

        }.exec(
            http("fetch from ${schema}")
              .get(s"/resources/perftestorg/perftestproj$project/$${encodedSchema}/$${encodedId}")
              .check(bodyString.saveAs("savedPayload"))
          )
          .exec { session =>
            val json     = parse(session.get("savedPayload").as[String]).right.get
            val revision = json.asObject.getOrElse(JsonObject())("_rev").flatMap(_.asNumber).flatMap(_.toInt).get
            val update = json.mapObject { obj =>
              obj
                .filterKeys(s => !s.startsWith("_"))
                .add(s"nxv:updated${revision + 1}", Json.fromString(s"${UUID.randomUUID().toString}"))
            }
            session.set("updateRevision", revision).set("updatePayload", update.spaces2)
          }
          .exec(
            http("update ${schema}")
              .put(
                s"/resources/perftestorg/perftestproj$project/$${encodedSchema}/$${encodedId}?rev=$${updateRevision}")
              .body(StringBody("${updatePayload}"))
              .header("Content-Type", "application/json")
          )
      ))
//  setUp(scn.inject(constantUsersPerSec(1) during (30 seconds)).protocols(httpConf))

  setUp(scn.inject(atOnceUsers(12)).protocols(httpConf))

}
