package cortex.domain.auth

trait UserRepository[F[_]]:
  def find(id: UserId): F[Option[User]]
  def create(user: User): F[User]
  def delete(id: UserId): F[Boolean]
