package cortex.domain.auth

import cortex.domain.users.UserId

trait RefreshTokenService[F[_]]:
  def issue(userId: UserId): F[RefreshToken]
  def rotate(token: RefreshToken): F[Either[RefreshTokenError, (UserId, RefreshToken)]]
  def revoke(token: RefreshToken): F[Boolean]
