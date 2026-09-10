package cortex.application.auth

import cats.effect.{IO, Ref}
import cats.effect.testing.scalatest.AsyncIOSpec
import cats.syntax.all.*
import cortex.domain.users.{Email, HashedPassword, User, UserId, UserRepository}
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers
import tsec.passwordhashers.PasswordHash
import tsec.passwordhashers.jca.BCrypt

import java.nio.charset.StandardCharsets.UTF_8
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

class RegisterUserSpec extends AsyncFlatSpec, AsyncIOSpec, Matchers:
  private val email = Email("reader@example.com")
  private val password = "пароль-correct-horse-battery"
  private val existingUser = User.make(
    UserId(UUID.fromString("00000000-0000-0000-0000-000000000001")),
    email,
    HashedPassword("unused"),
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

  it should "persist a user with a verifiable BCrypt hash and return the saved user" in:
    for
      saved <- Ref.of[IO, Option[User]](None)
      repository = new StubRepository:
        override def findByEmail(requested: Email): IO[Option[User]] =
          IO(assert(requested == email)).as(None)
        override def create(user: User): IO[User] = saved.set(Some(user)).as(user)
      before <- IO.realTimeInstant
      result <- registerUser(repository, email.value, password)
      after <- IO.realTimeInstant
      stored <- saved.get
      user <- IO.fromEither(result.leftMap(error => new AssertionError(s"Registration failed: $error")))
      matches <- BCrypt.checkpwBool[IO](password.getBytes(UTF_8), PasswordHash[BCrypt](user.hashedPassword.value))
      wrongMatches <- BCrypt.checkpwBool[IO](
                        "секрет-correct-horse-battery".getBytes(UTF_8),
                        PasswordHash[BCrypt](user.hashedPassword.value)
                      )
    yield
      stored shouldBe Some(user)
      user.email shouldBe email
      user.id.value.version() shouldBe 4
      user.hashedPassword.value should not be password
      matches shouldBe true
      wrongMatches shouldBe false
      user.createdAt.getNano % 1000 shouldBe 0
      user.createdAt.isBefore(before.truncatedTo(ChronoUnit.MICROS)) shouldBe false
      user.createdAt.isAfter(after) shouldBe false

  it should "reject an invalid email without accessing the repository" in:
    registerUser(new StubRepository, "invalid", password).map: result =>
      result shouldBe Left(RegisterError.InvalidEmail("Invalid email address: invalid"))

  it should "reject empty or oversized passwords before accessing the repository" in:
    List("", "a" * 73, "я" * 37).traverse: invalidPassword =>
      registerUser(new StubRepository, email.value, invalidPassword).map: result =>
        result shouldBe Left(RegisterError.InvalidPassword("Password must contain between 1 and 72 UTF-8 bytes"))
    .map(_ => succeed)

  it should "accept a password of exactly 72 UTF-8 bytes" in:
    val repository = new StubRepository:
      override def findByEmail(email: Email): IO[Option[User]] = IO.pure(None)
      override def create(user: User): IO[User] = IO.pure(user)

    registerUser(repository, email.value, "я" * 36).flatMap:
      case Left(error) => IO(fail(s"Registration failed: $error"))
      case Right(user) =>
        BCrypt.checkpwBool[IO](("я" * 36).getBytes(UTF_8), PasswordHash[BCrypt](user.hashedPassword.value)).map: matches =>
          matches shouldBe true

  it should "reject an existing email without creating another user" in:
    val repository = new StubRepository:
      override def findByEmail(email: Email): IO[Option[User]] = IO.pure(Some(existingUser))

    registerUser(repository, email.value, password).map: result =>
      result shouldBe Left(RegisterError.EmailAlreadyExists(email))

  it should "propagate lookup failures through IO" in:
    val failure = new RuntimeException("Lookup unavailable")
    val repository = new StubRepository:
      override def findByEmail(email: Email): IO[Option[User]] = IO.raiseError(failure)

    registerUser(repository, email.value, password).attempt.map: result =>
      result shouldBe Left(failure)

  it should "propagate persistence failures through IO" in:
    val failure = new RuntimeException("Insert failed")
    val repository = new StubRepository:
      override def findByEmail(email: Email): IO[Option[User]] = IO.pure(None)
      override def create(user: User): IO[User] = IO.raiseError(failure)

    registerUser(repository, email.value, password).attempt.map: result =>
      result shouldBe Left(failure)
