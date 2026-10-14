package cortex.application.auth

import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import cortex.domain.users.{Email, User}
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers

class LoginUserSpec extends AsyncFlatSpec, AsyncIOSpec, Matchers, AuthFixture:
  it should "return the existing user when the password is correct" in:
    for
      user   <- makeUser
      result <- loginUser(repositoryReturning(Some(user)), passwordHasher, email.value, password)
    yield result shouldBe Right(user)

  it should "return InvalidCredentials for an incorrect password" in:
    for
      user   <- makeUser
      result <- loginUser(repositoryReturning(Some(user)), passwordHasher, email.value, wrongPassword)
    yield result shouldBe Left(LoginError.InvalidCredentials)

  it should "return InvalidCredentials for an email with no registered user" in:
    loginUser(repositoryReturning(None), passwordHasher, email.value, password).map: result =>
      result shouldBe Left(LoginError.InvalidCredentials)

  it should "reject an invalid email without accessing the repository" in:
    loginUser(new StubUserRepository, passwordHasher, "invalid", password).map: result =>
      result shouldBe Left(LoginError.InvalidEmail("Invalid email address: invalid"))

  it should "reject an invalid password without accessing the repository" in:
    loginUser(new StubUserRepository, passwordHasher, email.value, "").map: result =>
      result shouldBe Left(LoginError.InvalidPassword(passwordLengthError))

  it should "propagate lookup failures through IO" in:
    val failure    = new RuntimeException("Lookup unavailable")
    val repository = new StubUserRepository:
      override def findByEmail(email: Email): IO[Option[User]] = IO.raiseError(failure)

    loginUser(repository, passwordHasher, email.value, password).attempt.map: result =>
      result shouldBe Left(failure)
