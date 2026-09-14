package cortex.application.auth

import cortex.domain.users.HashedPassword

trait PasswordHasher[F[_]]:
  def hash(rawPassword: String): F[HashedPassword]
  def verify(rawPassword: String, hash: HashedPassword): F[Boolean]