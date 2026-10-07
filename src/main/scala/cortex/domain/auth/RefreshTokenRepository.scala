package cortex.domain.auth

import java.time.Instant

trait RefreshTokenRepository[F[_]]:
  def create(token: RefreshTokenRecord): F[Unit]

  def findActiveByHash(
    tokenHash: RefreshTokenHash,
    now: Instant
  ): F[Option[RefreshTokenRecord]]

  def revoke(
    id: RefreshTokenId,
    revokedAt: Instant
  ): F[Boolean]

  def rotate(
    oldHash: RefreshTokenHash,
    replacement: RefreshTokenRecord,
    now: Instant
  ): F[Boolean]
