package cortex.infrastructure.auth

import cortex.domain.auth.{RefreshTokenHash, RefreshTokenId}
import doobie.postgres.implicits.UuidType
import doobie.util.meta.Meta

object DoobieAuthMappings:
  given Meta[RefreshTokenHash] = Meta[String].timap(RefreshTokenHash.fromHash)(_.value)
  given Meta[RefreshTokenId] = Meta[java.util.UUID].timap(RefreshTokenId.fromUUID)(_.value)
