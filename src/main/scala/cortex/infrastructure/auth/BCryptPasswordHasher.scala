package cortex.infrastructure.auth

import cats.effect.kernel.Async
import cats.syntax.all.*
import tsec.passwordhashers.PasswordHash
import tsec.passwordhashers.jca.BCrypt
import cortex.application.auth.PasswordHasher
import cortex.domain.users.HashedPassword

import java.nio.charset.StandardCharsets.UTF_8

final private[infrastructure] class BCryptPasswordHasher[F[_]: Async] extends PasswordHasher[F]:

  override def hash(rawPassword: String): F[HashedPassword] =
    BCrypt
      .hashpw[F](rawPassword.getBytes(UTF_8))
      .map(HashedPassword(_))

  override def verify(
    rawPassword: String,
    hash: HashedPassword
  ): F[Boolean] =
    BCrypt.checkpwBool[F](
      rawPassword.getBytes(UTF_8),
      PasswordHash[BCrypt](hash.value)
    )

object BCryptPasswordHasher:
  def apply[F[_]: Async]: PasswordHasher[F] =
    new BCryptPasswordHasher[F]
