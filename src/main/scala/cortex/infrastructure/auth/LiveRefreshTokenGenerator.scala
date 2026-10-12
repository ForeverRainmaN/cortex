package cortex.infrastructure.auth

import cats.effect.Sync
import cortex.domain.auth.{RefreshToken, RefreshTokenGenerator}

import java.security.SecureRandom
import java.util.Base64

final private[infrastructure] class LiveRefreshTokenGenerator[F[_]: Sync] private extends RefreshTokenGenerator[F]:
  private val random  = new SecureRandom()
  private val encoder = Base64.getUrlEncoder.withoutPadding()

  override def generate: F[RefreshToken] =
    Sync[F].blocking:
      val bytes = new Array[Byte](32)
      random.nextBytes(bytes)
      RefreshToken(encoder.encodeToString(bytes))

object LiveRefreshTokenGenerator:
  def apply[F[_]: Sync]: F[RefreshTokenGenerator[F]] = Sync[F].blocking[RefreshTokenGenerator[F]]:
    new LiveRefreshTokenGenerator[F]
