package cortex.infrastructure.users

import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import cortex.domain.users.{User, UserId}
import cortex.infrastructure.users.DoobieUserRepository
import doobie.implicits.*
import doobie.postgres.implicits.*
import doobie.syntax.*
import org.postgresql.util.PSQLException
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers

class DoobieUserRepositorySpec extends AsyncFlatSpec, AsyncIOSpec, DoobieSpec, Matchers, DoobieUserRepositoryFixture:
  val initScript: String = "sql/users.sql"

  it should "create new user" in:
    transactor.use: xa =>
      val program =
        for
          users     <- DoobieUserRepository[IO](xa)
          _         <- users.create(testUser)
          maybeUser <- sql"SELECT * FROM users WHERE user_id = ${testUser.id}"
                         .query[User]
                         .option
                         .transact(xa)
        yield maybeUser
      program.map: user =>
        user shouldBe Some(testUser)

  it should "fail to create a user if email already exists" in:
    withUsersRepository: users =>
      for
        _        <- users.create(testUser)
        duplicate = testUser.copy(id = UserId.generate)
        result   <- users.create(duplicate).attempt
      yield result match
        case Left(e: PSQLException) => e.getSQLState shouldBe "23505"
        case _                      => fail("Expected duplicate email error")

  it should "retrieve a user by id" in:
    withUsersRepository: users =>
      for
        _         <- users.create(testUser)
        maybeUser <- users.find(testUser.id)
      yield maybeUser shouldBe Some(testUser)

  it should "return None if trying to retrieve a user that does not exist" in:
    withUsersRepository: users =>
      for maybeUser <- users.find(testUser.id)
      yield maybeUser shouldBe None

  it should "delete user by id" in:
    withUsersRepository: users =>
      for
        _      <- users.create(testUser)
        result <- users.delete(testUser.id)
      yield result shouldBe true

  it should "NOT delete a user that does not exist" in:
    withUsersRepository: users =>
      for result <- users.delete(testUser.id)
      yield result shouldBe false
