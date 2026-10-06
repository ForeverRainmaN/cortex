package cortex.infrastructure.auth

import cats.data.OptionT
import cortex.domain.users.{User, UserId, UserRepository}
import tsec.authentication.IdentityStore

object UserIdentityStore:
  def make[F[_]](
    users: UserRepository[F]
  ): IdentityStore[F, UserId, User] =
    (userId: UserId) => OptionT(users.find(userId))
