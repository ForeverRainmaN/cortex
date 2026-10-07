package cortex.infrastructure.auth

import cortex.domain.auth.{RefreshTokenHash, RefreshTokenId}
import doobie.postgres.implicits.UuidType
import doobie.util.meta.Meta

given Meta[RefreshTokenHash] = Meta[String].timap(RefreshTokenHash(_))(_.value)
given Meta[RefreshTokenId] = Meta[java.util.UUID].timap(RefreshTokenId(_))(_.value)
