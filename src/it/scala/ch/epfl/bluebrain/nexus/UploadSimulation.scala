package ch.epfl.bluebrain.nexus

import java.net.URLEncoder

import akka.http.scaladsl.model.Uri
import ch.epfl.bluebrain.nexus.config.Settings
import ch.epfl.bluebrain.nexus.data.generation.ResourcesGenerator
import ch.epfl.bluebrain.nexus.data.generation.types.{Settings => GenerationSettings}
import com.typesafe.config.ConfigFactory
import io.circe.Json
import io.circe.parser.parse
import io.gatling.core.Predef._
import io.gatling.http.Predef._

class UploadSimulation extends Simulation {

  val config        = new Settings(ConfigFactory.parseResources("perf-tests.conf").resolve()).appConfig
  val numProjects   = config.uploadConfig.projects
  val parallelUsers = config.uploadConfig.parallelUsers

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
  val settings: GenerationSettings = GenerationSettings(Uri("http://example.com/ids/"), map)

  val data = 1 to numProjects flatMap { i =>
    ResourcesGenerator(1, Math.pow(10, i).toInt, 20)(settings).right.get.map((i, _))
  }

  val feeder = data
    .flatMap {
      case (project, instance) if instance.schema.toString() == "https://bluebrain.github.io/nexus/schemas/experiment/wholecellpatchclamp" =>
        List(
          Map(
            "payload"          -> instance.payload,
            "project"          -> project,
            "schema"           -> URLEncoder.encode(instance.schema.toString, "UTF-8"),
            "schemaNonEncoded" -> instance.schema.toString
          ),
          Map(
            "payload"          -> parse(instance.payload).right.get.mapObject{ obj =>
              obj.add("@id", Json.fromString(s"${obj("@id").get.asString.get}/resource"))
            },
            "project"          -> project,
            "schema"           -> "resource",
            "schemaNonEncoded" -> "resource"
          )
        )
      case (project, instance) =>
        List(
          Map(
            "payload"          -> instance.payload,
            "project"          -> project,
            "schema"           -> URLEncoder.encode(instance.schema.toString, "UTF-8"),
            "schemaNonEncoded" -> instance.schema.toString
          ))
    }
    .toArray
    .queue

  val httpConf = http
    .baseURL(config.kg.base.toString) // Here is the root for all relative URLs
    .authorizationHeader(s"Bearer ${config.http.token}")

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

  setUp(scn.inject(atOnceUsers(parallelUsers)).protocols(httpConf))
}
