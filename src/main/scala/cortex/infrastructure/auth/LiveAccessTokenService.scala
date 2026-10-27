package cortex.infrastructure.auth

import pdi.jwt.{JwtAlgorithm, JwtCirce, JwtClaim}

import cortex.config.SecurityConfig
import cortex.domain.auth.{AccessToken, AccessTokenService}
import cortex.domain.users.UserId

final class LiveAccessTokenService[F[_]](
  securityConfig: SecurityConfig
) extends AccessTokenService[F]:
  override def issue(userId: UserId): F[AccessToken] = ???
