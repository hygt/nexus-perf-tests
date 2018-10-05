package ch.epfl.bluebrain.nexus.data.generation

import java.util.UUID
import java.util.concurrent.atomic.AtomicLong
import java.util.regex.Pattern

import ammonite.ops._
import ch.epfl.bluebrain.nexus.commons.test.Randomness._
import ch.epfl.bluebrain.nexus.data.generation.types.Data.LocalData
import ch.epfl.bluebrain.nexus.data.generation.types.Settings
import ch.epfl.bluebrain.nexus.service.http.UriOps._
import ch.epfl.bluebrain.nexus.service.http.{Path => Addr}

import scala.collection.concurrent.TrieMap
import scala.collection.mutable

/**
  * Class that generates resources randomly based on a provided provenance templates.
  *
  * @param templates the provided provenance templates
  */
class ResourcesGenerator(templates: Stream[LocalData], ids: TrieMap[String, AtomicLong])(implicit settings: Settings) {

  /**
    * Generates a number of resources equal to ''total'' randomly generated from the ''provTemplate''
    *
    * @param total the amount of resources to be generated
    */
  final def apply(total: Int): Stream[LocalData] = {
    val x: Int = total / 20
    val map    = mutable.Map.empty[String, String]
    templates
      .flatMap { data => generate(numOfDuplicates(data.path, x), data, map)
      }
      .map(_.withReplacement(map))
  }

  private def generate(times: Int, data: LocalData, map: mutable.Map[String, String]): Stream[LocalData] = {
    (1 to times).toStream.map { _ =>
      val instanceId = ids.getOrElseUpdate(data.schema.toString, new AtomicLong).incrementAndGet
      val newId      = data.schema.append(Addr(s"ids/$instanceId")).toString
      map.update(data.id, newId)
      data.copy(id = newId, payload = replace(data.payload, data.id, newId))
    }
  }

  private def replace(payload: String, id: String, newId: String): String = {
    val replacements = Map(
      id                                     -> newId,
      "{{agent_givenName}}"                  -> s"Agent $uuid",
      "{{patchedslice_name}}"                -> s"Name $uuid",
      "{{patchedslice_providerId}}"          -> genInt().toString,
      "{{slice_name}}"                       -> s"Name $uuid",
      "{{slice_providerId}}"                 -> genInt().toString,
      """"{{subject_age}}""""                -> (genInt(60).toDouble + 0.5).toString,
      "{{subject_name}}"                     -> s"Name $uuid",
      "{{subject_providerId}}"               -> genInt().toString,
      "{{patchedcellcollection_name}}"       -> genInt().toString,
      "{{patchedcellcollection_providerId}}" -> genInt().toString,
      "{{name_protocol}}"                    -> s"Name $uuid",
      "{{brainslicing_brainregion_id}}"      -> uuid,
      "{{patchedcell_name}}"                 -> s"Name $uuid",
      "{{trace_stimulation_name}}"           -> s"Name $uuid",
      "{{trace_response_name}}"              -> s"Name $uuid",
      "{{trace_response_originalFileName}}"  -> uuid
    )
    replacements.foldLeft(payload) {
      case (acc, (original, replacement)) =>
        acc.replaceAll(Pattern.quote(original), replacement)
    }
  }

  private def numOfDuplicates(path: Path, factor: Int): Int = {
    (path / up).name match {
      case "trace"           => factor * 4
      case "tracegeneration" => factor * 2
      case _                 => factor
    }
  }

  private def uuid: String = UUID.randomUUID().toString

}

object ResourcesGenerator {

  /**
    * Generates a number of resources equal to ''total'' randomly generated from the ''provTemplate''
    *
    * @param orgs           the amount of organizations to be created
    * @param provTemplates  the amount of prov templates.
    * @param resources      the amount of resources to be created per prov template. This value should be multiple of 20.
    */
  final def apply(templates: Templates, orgs: Int, provTemplates: Int, resources: Int)(
      implicit settings: Settings): Stream[LocalData] = {
    require(resources % 20 == 0, "Amount of resources should be multiple of 20")

    val ids = TrieMap.empty[String, AtomicLong]
    Stream.fill(orgs)(genString(length = 6)).flatMap { org =>
      val transformed = transform(org, templates, provTemplates, resources)
      new ResourcesGenerator(transformed, ids).apply(resources)
    }
  }

  private def transform(org: String, templates: Templates, provTemplates: Int, resources: Int): Stream[LocalData] = {
    Stream
      .fill(provTemplates)((genString(length = 8), genString(length = 8), genString(length = 8)))
      .flatMap {
        case (core, electro, experiment) =>
          templates.value.toStream.map {
            case data if data.project == "core"              => data.copy(project = core, org = org)
            case data if data.project == "electrophysiology" => data.copy(project = electro, org = org)
            case data if data.project == "experiment"        => data.copy(project = experiment, org = org)
          }
      }
  }

}
