package cortex.infrastructure.users

import cats.effect.kernel.Async
import cats.implicits.*
import cortex.domain.users.{Email, User, UserId, UserRepository}
import doobie.{Fragment, Transactor}
import doobie.implicits.*
import doobie.postgres.implicits.*
import cortex.infrastructure.persistence.DoobieMappings.given
import org.typelevel.log4cats.Logger

final private[infrastructure] class DoobieUserRepository[F[_]: {Async, Logger}] private (xa: Transactor[F])
  extends UserRepository[F]:
  override def find(id: UserId): F[Option[User]] =
    selectUserBy(fr"user_id = $id")

  override def findByEmail(email: Email): F[Option[User]] =
    selectUserBy(fr"email = $email")

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

  private def selectUserBy(condition: Fragment): F[Option[User]] =
    (fr"SELECT user_id, email, hashed_password, created_at FROM users WHERE" ++
      condition)
      .query[User]
      .option
      .transact(xa)

object DoobieUserRepository:
  def apply[F[_]: {Async, Logger}](xa: Transactor[F]): UserRepository[F] =
    new DoobieUserRepository[F](xa)
