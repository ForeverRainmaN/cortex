package cortex.domain.auth

import cortex.domain.users.UserId

final case class RefreshTokenRotation(
  userId: UserId,
  refreshToken: RefreshToken
)

trait RefreshTokenService[F[_]]:
  def issue(userId: UserId): F[RefreshToken]
  def rotate(token: RefreshToken): F[Either[RefreshTokenError, RefreshTokenRotation]]
  def revoke(token: RefreshToken): F[Boolean]
