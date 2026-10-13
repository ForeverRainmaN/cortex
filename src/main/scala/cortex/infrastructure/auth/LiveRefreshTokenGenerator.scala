package cortex.infrastructure.auth

import cats.Functor
import cats.effect.Sync
import cats.effect.std.SecureRandom
import cats.syntax.functor.*
import cortex.domain.auth.{RefreshToken, RefreshTokenGenerator}

import java.util.Base64

final private[infrastructure] class LiveRefreshTokenGenerator[F[_]: Functor] private (
  random: SecureRandom[F]
) extends RefreshTokenGenerator[F]:
  import LiveRefreshTokenGenerator.{encoder, TokenSizeBytes}

  override def generate: F[RefreshToken] =
    random.nextBytes(TokenSizeBytes).map(bytes => RefreshToken(encoder.encodeToString(bytes)))

object LiveRefreshTokenGenerator:
  private val TokenSizeBytes = 32
  private val encoder        = Base64.getUrlEncoder.withoutPadding()

  def apply[F[_]: Sync]: F[RefreshTokenGenerator[F]] =
    SecureRandom.javaSecuritySecureRandom[F].map(new LiveRefreshTokenGenerator[F](_))
