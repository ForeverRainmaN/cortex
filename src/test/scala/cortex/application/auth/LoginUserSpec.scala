package cortex.application.auth

import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import cortex.domain.users.{Email, HashedPassword, User, UserId, UserRepository}
import cortex.infrastructure.auth.BCryptPasswordHasher
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers
import java.time.Instant
import java.util.UUID

class LoginUserSpec extends AsyncFlatSpec, AsyncIOSpec, Matchers:
  private val passwordHasher: PasswordHasher[IO] = BCryptPasswordHasher[IO]
  private val email = Email("reader@example.com")
  private val password = "пароль-correct-horse-battery"

  private def makeUser: IO[User] =
    passwordHasher.hash(password).map: hash =>
      User.make(
        UserId(UUID.fromString("00000000-0000-0000-0000-000000000001")),
        email,
        hash,
        Instant.EPOCH
      )

  private class StubRepository extends UserRepository[IO]:
    override def find(id: UserId): IO[Option[User]] =
      IO.raiseError(new AssertionError("Unexpected find"))
    override def findByEmail(email: Email): IO[Option[User]] =
      IO.raiseError(new AssertionError("Unexpected findByEmail"))
    override def create(user: User): IO[User] =
      IO.raiseError(new AssertionError("Unexpected create"))
    override def delete(id: UserId): IO[Boolean] =
      IO.raiseError(new AssertionError("Unexpected delete"))

  private def repositoryReturning(user: Option[User]): UserRepository[IO] =
    new StubRepository:
      override def findByEmail(requested: Email): IO[Option[User]] =
        IO(assert(requested == email)).as(user)

  it should "return the existing user when the password is correct" in:
    for
      user <- makeUser
      result <- loginUser(repositoryReturning(Some(user)), passwordHasher, email.value, password)
    yield result shouldBe Right(user)

  it should "return InvalidCredentials for an incorrect password" in:
    for
      user <- makeUser
      result <- loginUser(
        repositoryReturning(Some(user)),
        passwordHasher,
        email.value,
        "секрет-correct-horse-battery"
      )
    yield result shouldBe Left(LoginError.InvalidCredentials)

  it should "return InvalidCredentials for an email with no registered user" in:
    loginUser(repositoryReturning(None), passwordHasher, email.value, password).map: result =>
      result shouldBe Left(LoginError.InvalidCredentials)

  it should "reject an invalid email without accessing the repository" in:
    loginUser(new StubRepository, passwordHasher, "invalid", password).map: result =>
      result shouldBe Left(LoginError.InvalidEmail("Invalid email address: invalid"))

  it should "reject an invalid password without accessing the repository" in:
    loginUser(new StubRepository, passwordHasher, email.value, "").map: result =>
      result shouldBe Left(LoginError.InvalidPassword("Password must contain between 1 and 72 UTF-8 bytes"))

  it should "propagate lookup failures through IO" in:
    val failure = new RuntimeException("Lookup unavailable")
    val repository = new StubRepository:
      override def findByEmail(email: Email): IO[Option[User]] = IO.raiseError(failure)

    loginUser(repository, passwordHasher, email.value, password).attempt.map: result =>
      result shouldBe Left(failure)
