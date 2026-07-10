package cortex.infrastructure.auth

import cortex.domain.auth.{Email, HashedPassword, UserId}
import doobie.postgres.implicits.*
import doobie.util.meta.Meta

import java.util.UUID

given Meta[UserId] = Meta[UUID].timap(UserId(_))(_.value)

given Meta[Email] = Meta[String].timap(Email(_))(_.value)

given Meta[HashedPassword] = Meta[String].timap(HashedPassword(_))(_.value)
