package ch.epfl.bluebrain.nexus.config

import AppConfig._
import akka.actor.{ExtendedActorSystem, Extension, ExtensionId, ExtensionIdProvider}
import akka.http.scaladsl.model.Uri
import com.typesafe.config.Config
import pureconfig.ConvertHelpers._
import pureconfig.{ConfigConvert, loadConfigOrThrow}

/**
  * Akka settings extension to expose application configuration.  It typically uses the configuration instance of the
  * actor system as the configuration root.
  *
  * @param config the configuration instance to read
  */
@SuppressWarnings(Array("LooksLikeInterpolatedString"))
class Settings(config: Config) extends Extension {

  private implicit val uriConverter: ConfigConvert[Uri] =
    ConfigConvert.viaString[Uri](catchReadError(s => Uri(s)), _.toString)

  val appConfig = AppConfig(
    loadConfigOrThrow[HttpConfig](config, "app.http"),
    loadConfigOrThrow[KgConfig](config, "app.kg"),
    loadConfigOrThrow[UploadConfig](config, "app.upload")
  )

}

object Settings extends ExtensionId[Settings] with ExtensionIdProvider {
  // $COVERAGE-OFF$
  override def lookup(): ExtensionId[_ <: Extension] = Settings

  override def createExtension(system: ExtendedActorSystem): Settings = new Settings(system.settings.config)

  def apply(config: Config): Settings = new Settings(config)
  // $COVERAGE-ON$
}
