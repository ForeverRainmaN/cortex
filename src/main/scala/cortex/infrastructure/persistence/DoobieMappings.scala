package cortex.infrastructure.persistence

import cortex.domain.users.UserId
import doobie.util.meta.Meta
import doobie.postgres.implicits.UuidType

import java.util.UUID

object DoobieMappings:
  given Meta[UserId] = Meta[UUID].timap(UserId(_))(_.value)