package cortex.infrastructure.auth

import cats.effect.Async
import cortex.domain.auth.{RefreshToken, RefreshTokenGenerator}

import java.security.SecureRandom
import java.util.Base64

final private[infrastructure] class LiveRefreshTokenGenerator[F[_]: Async] private extends RefreshTokenGenerator[F]:
  private val random  = new SecureRandom()
  private val encoder = Base64.getUrlEncoder.withoutPadding()

  override def generate: F[RefreshToken] =
    Async[F].blocking:
      val bytes = new Array[Byte](32)
      random.nextBytes(bytes)
      RefreshToken(encoder.encodeToString(bytes))

object LiveRefreshTokenGenerator:
  def apply[F[_]: Async]: F[RefreshTokenGenerator[F]] = Async[F].blocking[RefreshTokenGenerator[F]]:
    new LiveRefreshTokenGenerator[F]
