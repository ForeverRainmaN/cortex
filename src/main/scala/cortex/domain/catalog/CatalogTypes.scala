package cortex.domain.catalog

import java.util.UUID
import scala.util.Try

opaque type ResourceId = UUID

object ResourceId:
  private def apply(value: UUID): ResourceId = value
  
  def fromUUID(value: UUID): ResourceId = apply(value)

  def fromString(value: String): Either[Throwable, ResourceId] =
    Try(UUID.fromString(value)).toEither.map(apply)

  extension (resourceId: ResourceId)
    def value: UUID = resourceId
