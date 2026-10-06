package cortex.infrastructure.users

import cortex.domain.users.{Email, HashedPassword}
import doobie.util.meta.Meta

given Meta[Email] = Meta[String].timap(Email(_))(_.value)

given Meta[HashedPassword] = Meta[String].timap(HashedPassword(_))(_.value)
