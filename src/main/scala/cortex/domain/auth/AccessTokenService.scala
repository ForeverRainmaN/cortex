package cortex.domain.auth

import cortex.domain.users.UserId

trait AccessTokenService[F[_]]:
  def issue(userId: UserId): F[AccessToken]
