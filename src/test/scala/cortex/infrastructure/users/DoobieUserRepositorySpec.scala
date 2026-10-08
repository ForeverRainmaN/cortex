package cortex.infrastructure.users

import cats.effect.IO
import cats.effect.std.UUIDGen
import cats.effect.testing.scalatest.AsyncIOSpec
import cortex.domain.users.{Email, User, UserId}
import cortex.infrastructure.persistence.DoobieMappings.given
import cortex.infrastructure.persistence.DoobieSpec
import doobie.implicits.*
import doobie.postgres.implicits.*
import org.postgresql.util.PSQLException
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers

class DoobieUserRepositorySpec extends AsyncFlatSpec, AsyncIOSpec, DoobieSpec, Matchers, DoobieUserRepositoryFixture:
  it should "create new user" in:
    transactor.use: xa =>
      val program =
        for
          users     <- DoobieUserRepository[IO](xa)
          _         <- users.create(user)
          maybeUser <- sql"SELECT * FROM users WHERE user_id = ${user.id}"
                         .query[User]
                         .option
                         .transact(xa)
        yield maybeUser
      program.map: savedUser =>
        savedUser shouldBe Some(user)

  it should "fail to create a user if email already exists" in:
    withRepository(DoobieUserRepository[IO](_)): users =>
      for
        _           <- users.create(user)
        duplicateId <- UUIDGen.randomUUID[IO].map(UserId(_))
        duplicate    = user.copy(id = duplicateId)
        result      <- users.create(duplicate).attempt
      yield result match
        case Left(e: PSQLException) => e.getSQLState shouldBe "23505"
        case _                      => fail("Expected duplicate email error")

  it should "retrieve a user by id" in:
    withRepository(DoobieUserRepository[IO](_)): users =>
      for
        _         <- users.create(user)
        maybeUser <- users.find(user.id)
      yield maybeUser shouldBe Some(user)

  it should "return None if trying to retrieve a user that does not exist" in:
    withRepository(DoobieUserRepository[IO](_)): users =>
      for maybeUser <- users.find(user.id)
      yield maybeUser shouldBe None

  it should "retrieve a user by email" in:
    withRepository(DoobieUserRepository[IO](_)): users =>
      for
        _       <- users.create(user)
        found   <- users.findByEmail(user.email)
        missing <- users.findByEmail(Email("missing@example.com"))
      yield
        found shouldBe Some(user)
        missing shouldBe None

  it should "delete user by id" in:
    withRepository(DoobieUserRepository[IO](_)): users =>
      for
        _      <- users.create(user)
        result <- users.delete(user.id)
      yield result shouldBe true

  it should "NOT delete a user that does not exist" in:
    withRepository(DoobieUserRepository[IO](_)): users =>
      for result <- users.delete(user.id)
      yield result shouldBe false
