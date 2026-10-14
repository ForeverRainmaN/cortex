package cortex.domain.catalog

import java.util.UUID
import scala.util.Try

opaque type ResourceId = UUID

object ResourceId:
  def apply(value: UUID): ResourceId = value

  def fromString(value: String): Either[Throwable, ResourceId] =
    Try(UUID.fromString(value)).toEither

  extension (resourceId: ResourceId)
    def value: UUID = resourceId
