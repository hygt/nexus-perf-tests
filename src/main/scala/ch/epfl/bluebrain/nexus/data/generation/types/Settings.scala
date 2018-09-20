package ch.epfl.bluebrain.nexus.data.generation.types

import akka.http.scaladsl.model.Uri

/**
  * Data type holding the ingestion settings.
  *
  * @param idPrefix    the prefix uri of the @id
  * @param nrOfThreads the number of threads used in [[scala.collection.parallel]]
  * @param schemasMap  map the string segment (of the path) to the schema @id value
  */
case class Settings(idPrefix: Uri, schemasMap: Map[String, Uri], nrOfThreads: Int = 4)
