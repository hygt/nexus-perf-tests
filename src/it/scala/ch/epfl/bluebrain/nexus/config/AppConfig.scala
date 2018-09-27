package ch.epfl.bluebrain.nexus.config

import AppConfig._
import akka.http.scaladsl.model.Uri

import scala.concurrent.duration.FiniteDuration


final case class AppConfig(http: HttpConfig, kg: KgConfig, uploadConfig: UploadConfig, fetchConfig: FetchConfig)

object AppConfig {

  final case class HttpConfig(token: String)

  final case class KgConfig(base: Uri)

  final case class UploadConfig(projects: Int, parallelUsers: Int)

  final case class FetchConfig(project: Int, duration: FiniteDuration, reads: Int, writes: Int, users: Int)

}
