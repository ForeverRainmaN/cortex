package cortex.domain.auth

import cortex.domain.users.UserId

import java.time.Instant

final case class RefreshTokenRecord(
  id: RefreshTokenId,
  userId: UserId,
  tokenHash: RefreshTokenHash,
  expiresAt: Instant,
  createdAt: Instant,
  revokedAt: Option[Instant]
):
  def isActive(now: Instant): Boolean =
    revokedAt.isEmpty && expiresAt.isAfter(now)
