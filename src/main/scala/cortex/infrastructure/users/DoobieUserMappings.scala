package cortex.infrastructure.users

import cortex.domain.users.{Email, HashedPassword}
import doobie.util.meta.Meta

object DoobieUserMappings:
  given Meta[Email] = Meta[String].timap(Email.fromStringUnsafe)(_.value)
  given Meta[HashedPassword] = Meta[String].timap(HashedPassword.fromHash)(_.value)
