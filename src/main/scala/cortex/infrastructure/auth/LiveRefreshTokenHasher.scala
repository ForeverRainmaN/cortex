package cortex.infrastructure.auth

import cats.effect.kernel.Sync
import cortex.domain.auth.{RefreshToken, RefreshTokenHash, RefreshTokenHasher}

import java.nio.charset.StandardCharsets.UTF_8
import java.security.MessageDigest
import java.util.Base64

final private[infrastructure] class LiveRefreshTokenHasher[F[_]: Sync] private extends RefreshTokenHasher[F]:

  override def hash(token: RefreshToken): F[RefreshTokenHash] =
    Sync[F].delay:
      val bytes = MessageDigest
        .getInstance("SHA-256")
        .digest(token.value.getBytes(UTF_8))

      val encoded =
        Base64.getUrlEncoder.withoutPadding().encodeToString(bytes)

      RefreshTokenHash(encoded)

object LiveRefreshTokenHasher:
  def apply[F[_]: Sync]: RefreshTokenHasher[F] =
    new LiveRefreshTokenHasher[F]
