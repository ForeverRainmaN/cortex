package cortex.infrastructure.auth

import cortex.domain.auth.RefreshTokenHash
import doobie.util.meta.Meta

given Meta[RefreshTokenHash] = Meta[String].timap(RefreshTokenHash(_))(_.value)
