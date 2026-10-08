package cortex.domain.auth

trait RefreshTokenHasher[F[_]]:
  def hash(token: RefreshToken): F[RefreshTokenHash]