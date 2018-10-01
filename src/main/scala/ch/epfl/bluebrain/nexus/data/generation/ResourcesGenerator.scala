package ch.epfl.bluebrain.nexus.data.generation

import java.util.UUID
import java.util.concurrent.atomic.AtomicLong
import java.util.regex.Pattern

import ammonite.ops._
import ch.epfl.bluebrain.nexus.commons.test.Randomness._
import ch.epfl.bluebrain.nexus.data.generation.types.Data.{FailedDataFormat, FailedDataSchemaMap, LocalData}
import ch.epfl.bluebrain.nexus.data.generation.types.Settings
import ch.epfl.bluebrain.nexus.service.http.UriOps._
import ch.epfl.bluebrain.nexus.service.http.{Path => Addr}
import journal.Logger

import scala.collection.concurrent.TrieMap
import scala.collection.mutable
import scala.collection.mutable.ListBuffer

/**
  * Class that generates resources randomly based on a provided provenance template.
  *
  * @param provTemplate  the provided provenance template
  */
class ResourcesGenerator(provTemplate: Seq[LocalData], ids: collection.concurrent.Map[String, AtomicLong])(
    implicit settings: Settings) {

  /**
    * Generates a number of resources equal to ''total'' randomly generated from the ''provTemplate''
    *
    * @param total the amount of resources to be generated
    */
  final def apply(total: Int): ListBuffer[LocalData] = {
    val x: Int = total / 20
    val (list, m) = provTemplate.foldLeft((ListBuffer.empty[LocalData], Map.empty[String, String])) {
      case ((accData, accMap), data) =>
        val (resL, resM) = generate(numOfDuplicates(data.path, x), data)
        (accData ++ resL, resM ++ accMap)
    }
    list.map(_.withReplacement(m))
  }

  private def generate(times: Int, data: LocalData): (ListBuffer[LocalData], Map[String, String]) = {
    val dataList = ListBuffer.empty[LocalData]
    val map      = mutable.Map.empty[String, String]
    (0 until times).foreach { _ =>
      val instanceId = ids.getOrElseUpdate(data.schema.toString(), new AtomicLong()).incrementAndGet()
      val newId      = data.schema.append(Addr(s"ids/$instanceId")).toString()
      val newData    = data.copy(id = newId, payload = replace(data.payload, data.id, newId))
      dataList += newData
      map.update(data.id, newId)
    }
    (dataList, map.toMap)
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
  val logger: Logger = Logger[this.type]

  /**
    * Generates a number of resources equal to ''total'' randomly generated from the ''provTemplate''
    *
    * @param orgs           the amount of organizations to be created
    * @param provTemplates  the amount of prov templates.
    * @param resources      the amount of resources to be created per prov template. This value should be multiple of 20.
    */
  final def apply(orgs: Int, provTemplates: Int, resources: Int)(
      implicit settings: Settings): Either[GenerationError, ListBuffer[LocalData]] = {
    val ids = TrieMap[String, AtomicLong]()
    if (resources % 20 != 0) Left(InvalidResourcesNumber)
    else
      provTemplate.map { template =>
        val acc = ListBuffer.empty[LocalData]
        List.fill(orgs)(genString(length = 6)).foreach { org =>
            List
              .fill(provTemplates)((genString(length = 8), genString(length = 8), genString(length = 8)))
              .foreach { case (core, electro, experiment) =>
                  val transformedTemplate = template.map {
                    case data if data.project == "core"              => data.copy(project = core, org = org)
                    case data if data.project == "electrophysiology" => data.copy(project = electro, org = org)
                    case data if data.project == "experiment"        => data.copy(project = experiment, org = org)
                  }
                  acc ++= new ResourcesGenerator(transformedTemplate, ids).apply(resources)
              }
        }
        acc
      }
  }

  private def provTemplate(implicit s: Settings): Either[GenerationError, ListBuffer[LocalData]] =
    (ls.rec ! pwd / "src" / "main" / "resources" / "bbp")
      .filter(_.isFile)
      .foldLeft[Either[GenerationError, ListBuffer[LocalData]]](Right(ListBuffer.empty)) {
        case (Right(acc), path) =>
          LocalData(path) match {
            case data: LocalData           => Right(acc :+ data)
            case data: FailedDataFormat    => Left(WrongFormat(data.path))
            case data: FailedDataSchemaMap => Left(SchemaNotMapped(data.path, data.schema))
          }
        case (err @ Left(_), _) => err
      }

  sealed abstract class GenerationError(message: String) extends Exception(message)

  final case object InvalidResourcesNumber extends GenerationError("Amount of resources should be multiple of 20")
  final case class SchemaNotMapped(resource: BasePath, schemas: String)
      extends GenerationError(s"Need to map the schema: '$schemas' in path '$resource'")
  final case class WrongFormat(resource: BasePath)
      extends GenerationError(s"Resource in path '$resource' is not in JSON format")

}
