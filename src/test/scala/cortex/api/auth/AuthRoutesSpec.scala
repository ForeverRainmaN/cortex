package cortex.api.auth

import cats.effect.{IO, Ref}
import cats.effect.testing.scalatest.AsyncIOSpec
import cats.syntax.all.*
import cortex.application.auth.AuthFixture
import cortex.domain.users.{CreateUserError, Email, User, UserRepository}
import io.circe.Json
import org.http4s.{Method, Request, Response, Status}
import org.http4s.circe.*
import org.http4s.implicits.*
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers
import sttp.tapir.server.http4s.Http4sServerInterpreter

class AuthRoutesSpec extends AsyncFlatSpec, AsyncIOSpec, Matchers, AuthFixture:

  private def payload(
    requestedEmail: String = email.value,
    requestedPassword: String = password
  ): Json =
    Json.obj(
      "email" -> Json.fromString(requestedEmail),
      "password" -> Json.fromString(requestedPassword)
    )

  private def register(repository: UserRepository[IO], body: Json): IO[Response[IO]] =
    val endpoints = new AuthRoutes[IO](repository, passwordHasher, stubAccessTokenService, stubRefreshTokenService).r
    val app       = Http4sServerInterpreter[IO]().toRoutes(endpoints.toList).orNotFound
    app.run(Request[IO](Method.POST, uri"/api/auth/register").withEntity(body))

  it should "return 201 and the saved user's public fields without exposing the password hash" in:
    for
      saved     <- Ref.of[IO, Option[User]](None)
      repository = new StubUserRepository:
                     override def findByEmail(email: Email): IO[Option[User]] = IO.pure(None)
                     override def create(user: User): IO[Either[CreateUserError, User]] =
                       saved.set(Some(user)).as(Right(user))
      response  <- register(repository, payload())
      body      <- response.as[Json]
      stored    <- saved.get
      user      <- IO.fromOption(stored)(new AssertionError("User was not saved"))
    yield
      response.status shouldBe Status.Created
      body shouldBe Json.obj(
        "userId" -> Json.fromString(user.id.value.toString),
        "email" -> Json.fromString(email.value)
      )
      user.hashedPassword.value should not be password

  it should "return 400 for an invalid email before accessing persistence" in:
    for
      response <- register(new StubUserRepository, payload(requestedEmail = "invalid"))
      body     <- response.as[Json]
    yield
      response.status shouldBe Status.BadRequest
      body.hcursor.get[String]("code") shouldBe Right("invalid_email")

  it should "return 400 for an invalid password before accessing persistence" in:
    for
      response <- register(new StubUserRepository, payload(requestedPassword = ""))
      body     <- response.as[Json]
    yield
      response.status shouldBe Status.BadRequest
      body.hcursor.get[String]("code") shouldBe Right("invalid_password")

  it should "return 409 when the email already exists" in:
    for
      response <- register(repositoryReturning(Some(existingUser)), payload())
      body     <- response.as[Json]
    yield
      response.status shouldBe Status.Conflict
      body.hcursor.get[String]("code") shouldBe Right("email_already_exists")

  it should "return 409 when another registration takes the email after the lookup" in:
    val repository = new StubUserRepository:
      override def findByEmail(email: Email): IO[Option[User]] = IO.pure(None)
      override def create(user: User): IO[Either[CreateUserError, User]] =
        IO.pure(Left(CreateUserError.EmailAlreadyExists(user.email)))

    for
      response <- register(repository, payload())
      body     <- response.as[Json]
    yield
      response.status shouldBe Status.Conflict
      body.hcursor.get[String]("code") shouldBe Right("email_already_exists")

  it should "return 400 for a missing password without invoking registration" in:
    register(new StubUserRepository, Json.obj("email" -> Json.fromString(email.value))).map: response =>
      response.status shouldBe Status.BadRequest
