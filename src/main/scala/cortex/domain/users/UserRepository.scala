package cortex.domain.users

trait UserRepository[F[_]]:
  def find(id: UserId): F[Option[User]]
  def findByEmail(email: Email): F[Option[User]]
  def create(user: User): F[Either[CreateUserError, User]]
  def delete(id: UserId): F[Boolean]
