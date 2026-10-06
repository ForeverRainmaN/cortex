package cortex.infrastructure.auth

import cats.effect.kernel.Async
import cortex.domain.auth.{RefreshToken, RefreshTokenError, RefreshTokenRepository, RefreshTokenService}
import cortex.domain.users.UserId

import scala.concurrent.duration.FiniteDuration
import java.security.SecureRandom
import java.util.Base64

final private[infrastructure] class LiveRefreshTokenService[F[_]: Async] private (
  repository: RefreshTokenRepository[F],
  refreshTokenTtl: FiniteDuration
) extends RefreshTokenService[F]:

  private val random  = new SecureRandom()
  private val encoder = Base64.getUrlEncoder.withoutPadding()

  override def issue(userId: UserId): F[RefreshToken] = ???

  override def rotate(
    token: RefreshToken
  ): F[Either[RefreshTokenError, (UserId, RefreshToken)]] = ???

  override def revoke(token: RefreshToken): F[Boolean] = ???

  private def generateToken: F[RefreshToken] =
    Async[F].delay:
      val bytes = new Array[Byte](32)
      random.nextBytes(bytes)
      RefreshToken(encoder.encodeToString(bytes))
