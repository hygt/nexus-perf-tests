package ch.epfl.bluebrain.nexus.config

import AppConfig._
import akka.http.scaladsl.model.Uri


final case class AppConfig(http: HttpConfig, kg: KgConfig, uploadConfig: UploadConfig)

object AppConfig {

  final case class HttpConfig(token: String)

  final case class KgConfig(base: Uri)

  final case class UploadConfig(projects: Int, parallelUsers: Int)

}
