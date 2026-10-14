package cortex.application.auth

import cats.effect.{Clock, IO, Ref}
import cats.effect.testing.scalatest.AsyncIOSpec
import cats.syntax.all.*
import cortex.domain.users.{CreateUserError, Email, User, UserRepository}
import cortex.testkit.ManualClock
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers

class RegisterUserSpec extends AsyncFlatSpec, AsyncIOSpec, Matchers, AuthFixture:
  private def register(
    repository: UserRepository[IO],
    email: String = email.value,
    password: String = password
  ): IO[Either[RegisterError, User]] =
    ManualClock.at(referenceTime).flatMap: clock =>
      given Clock[IO] = clock
      registerUser(repository, passwordHasher, email, password)

  it should "persist a user with a verifiable BCrypt hash and return the saved user" in:
    for
      saved        <- Ref.of[IO, Option[User]](None)
      repository    = new StubUserRepository:
                        override def findByEmail(requested: Email): IO[Option[User]] =
                          IO(assert(requested == email)).as(None)
                        override def create(user: User): IO[Either[CreateUserError, User]] =
                          saved.set(Some(user)).as(Right(user))
      result       <- register(repository)
      stored       <- saved.get
      user         <- IO.fromEither(result.leftMap(error => new AssertionError(s"Registration failed: $error")))
      matches      <- passwordHasher.verify(password, user.hashedPassword)
      wrongMatches <- passwordHasher.verify(wrongPassword, user.hashedPassword)
    yield
      stored shouldBe Some(user)
      user.email shouldBe email
      user.id.value.version() shouldBe 4
      user.hashedPassword.value should not be password
      matches shouldBe true
      wrongMatches shouldBe false
      user.createdAt shouldBe referenceTime

  it should "reject an invalid email without accessing the repository" in:
    register(new StubUserRepository, email = "invalid").map: result =>
      result shouldBe Left(RegisterError.InvalidEmail("Invalid email address: invalid"))

  it should "reject empty or oversized passwords before accessing the repository" in:
    List("", "a" * 73, "я" * 37)
      .traverse: invalidPassword =>
        register(new StubUserRepository, password = invalidPassword).map: result =>
          result shouldBe Left(RegisterError.InvalidPassword(passwordLengthError))
      .map(_ => succeed)

  it should "accept a password of exactly 72 UTF-8 bytes" in:
    val repository = new StubUserRepository:
      override def findByEmail(email: Email): IO[Option[User]]            = IO.pure(None)
      override def create(user: User): IO[Either[CreateUserError, User]] = IO.pure(Right(user))

    register(repository, password = "я" * 36).flatMap:
      case Left(error) => IO(fail(s"Registration failed: $error"))
      case Right(user) =>
        passwordHasher.verify("я" * 36, user.hashedPassword).map: matches =>
          matches shouldBe true

  it should "reject an existing email without creating another user" in:
    register(repositoryReturning(Some(existingUser))).map: result =>
      result shouldBe Left(RegisterError.EmailAlreadyExists(email))

  it should "return EmailAlreadyExists when the email is taken concurrently after the lookup" in:
    val repository = new StubUserRepository:
      override def findByEmail(email: Email): IO[Option[User]]            = IO.pure(None)
      override def create(user: User): IO[Either[CreateUserError, User]] =
        IO.pure(Left(CreateUserError.EmailAlreadyExists(user.email)))

    register(repository).map: result =>
      result shouldBe Left(RegisterError.EmailAlreadyExists(email))

  it should "propagate lookup failures through IO" in:
    val failure    = new RuntimeException("Lookup unavailable")
    val repository = new StubUserRepository:
      override def findByEmail(email: Email): IO[Option[User]] = IO.raiseError(failure)

    register(repository).attempt.map: result =>
      result shouldBe Left(failure)

  it should "propagate persistence failures through IO" in:
    val failure    = new RuntimeException("Insert failed")
    val repository = new StubUserRepository:
      override def findByEmail(email: Email): IO[Option[User]]            = IO.pure(None)
      override def create(user: User): IO[Either[CreateUserError, User]] = IO.raiseError(failure)

    register(repository).attempt.map: result =>
      result shouldBe Left(failure)
