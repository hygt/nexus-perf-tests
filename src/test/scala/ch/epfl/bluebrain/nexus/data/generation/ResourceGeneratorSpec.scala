package ch.epfl.bluebrain.nexus.data.generation

import akka.http.scaladsl.model.Uri
import ch.epfl.bluebrain.nexus.data.generation.ResourcesGenerator._
import ch.epfl.bluebrain.nexus.data.generation.types.Settings
import org.scalatest.{EitherValues, Matchers, WordSpecLike}

class ResourceGeneratorSpec extends WordSpecLike with Matchers with EitherValues {
  "A resource generator" should {
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
    val settings: Settings = Settings(Uri("https://bluebrain.github.io/nexus/schemas/"), map)

    "fail when resources is not multiple of 20" in {
      ResourcesGenerator(1, 1, 15)(settings).left.value shouldEqual InvalidResourcesNumber
    }

    "fail when schema is not present" in {
      ResourcesGenerator(1, 3, 20 * 3)(settings.copy(schemasMap = map - "person")).left.value shouldBe a[
        SchemaNotMapped]
    }

    "generate data randomly with the correct amount of resource distribution" in {
      val data = ResourcesGenerator(2, 3, 200)(settings).right.value
      data.size shouldEqual 1200
      val resultMap = data.map(d => d.org -> d.project).groupBy(_._1)
      resultMap.size shouldEqual 2
      resultMap.values.foreach(_.toSet.size shouldEqual 9)
      resultMap.values.foreach(_.size shouldEqual 200 * 3)
      data.foreach(_.id should startWith(settings.idPrefix.toString()))
      data.foreach(d => println(s"${d.schema} -> ${d.id} -> ${d.relationships}"))
    }
  }

}
