package cortex.infrastructure.users

import cats.effect.kernel.Async
import cats.implicits.*
import cortex.domain.users.{User, UserId, UserRepository}
import doobie.Transactor
import doobie.implicits.*
import doobie.postgres.implicits.*
import org.typelevel.log4cats.Logger

final class DoobieUserRepository[F[_]: {Async, Logger}] private (xa: Transactor[F]) extends UserRepository[F]:
  override def find(id: UserId): F[Option[User]] =
    sql"SELECT user_id, email, hashed_password, created_at FROM users WHERE user_id = $id"
      .query[User].option.transact[F](xa)

  override def create(user: User): F[User] =
    sql"""INSERT INTO users(
         user_id,
         email,
         hashed_password,
         created_at
       ) VALUES (
         ${user.id},
         ${user.email},
         ${user.hashedPassword},
         ${user.createdAt}
       )""".update.run.transact(xa).as(user)

  override def delete(id: UserId): F[Boolean] =
    sql"DELETE FROM users WHERE user_id = $id".update.run.transact(xa).map(_ > 0)

object DoobieUserRepository:
  def apply[F[_]: {Async, Logger}](xa: Transactor[F]): F[DoobieUserRepository[F]] =
    new DoobieUserRepository[F](xa).pure[F]
