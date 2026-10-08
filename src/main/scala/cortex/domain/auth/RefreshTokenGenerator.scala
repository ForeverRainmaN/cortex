package cortex.domain.auth

trait RefreshTokenGenerator[F[_]]:
  def generate: F[RefreshToken]
