package cortex.application.auth

import cats.effect.IO
import cortex.domain.auth.{AccessToken, AccessTokenService, RefreshToken, RefreshTokenError, RefreshTokenRotation, RefreshTokenService, TokenPair}
import cortex.domain.users.{CreateUserError, Email, HashedPassword, User, UserId, UserRepository}
import cortex.infrastructure.auth.BCryptPasswordHasher

import java.time.Instant
import java.util.UUID

trait AuthFixture:
  protected val passwordHasher: PasswordHasher[IO] = BCryptPasswordHasher[IO]

  protected val referenceTime: Instant = Instant.parse("2026-10-07T12:00:00Z")
  protected val userId: UserId         = UserId.fromUUID(UUID.fromString("00000000-0000-0000-0000-000000000001"))
  protected val email: Email           = Email.fromStringUnsafe("reader@example.com")
  protected val password               = "пароль-correct-horse-battery"
  protected val wrongPassword          = "секрет-correct-horse-battery"
  protected val passwordLengthError    = "Password must contain between 1 and 72 UTF-8 bytes"

  protected val existingUser: User =
    User.make(userId, email, HashedPassword.fromHash("unused"), referenceTime)

  protected def makeUser: IO[User] =
    passwordHasher.hash(password).map(User.make(userId, email, _, referenceTime))

  protected class StubUserRepository extends UserRepository[IO]:
    override def find(id: UserId): IO[Option[User]]                    =
      IO.raiseError(new AssertionError("Unexpected find"))
    override def findByEmail(email: Email): IO[Option[User]]           =
      IO.raiseError(new AssertionError("Unexpected findByEmail"))
    override def create(user: User): IO[Either[CreateUserError, User]] =
      IO.raiseError(new AssertionError("Unexpected create"))
    override def delete(id: UserId): IO[Boolean]                       =
      IO.raiseError(new AssertionError("Unexpected delete"))

  protected class StubAccessTokenService extends AccessTokenService[IO]:
    override def issue(userId: UserId): IO[AccessToken] =
      IO.pure(AccessToken.fromString("stub-access-token"))

  protected class StubRefreshTokenService extends RefreshTokenService[IO]:
    override def issue(userId: UserId): IO[RefreshToken] =
      IO.pure(RefreshToken.fromString("stub-refresh-token"))
    override def rotate(token: RefreshToken): IO[Either[RefreshTokenError, RefreshTokenRotation]] =
      IO.pure(Left(RefreshTokenError.InvalidOrExpired))
    override def revoke(token: RefreshToken): IO[Boolean] =
      IO.pure(false)

  protected val stubAccessTokenService: AccessTokenService[IO] = new StubAccessTokenService
  protected val stubRefreshTokenService: RefreshTokenService[IO] = new StubRefreshTokenService

  protected def repositoryReturning(user: Option[User]): UserRepository[IO] =
    new StubUserRepository:
      override def findByEmail(requested: Email): IO[Option[User]] =
        IO(assert(requested == email)).as(user)
