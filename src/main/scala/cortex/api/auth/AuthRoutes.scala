package cortex.api.auth

import cats.Monad
import cats.effect.Clock
import cats.effect.std.UUIDGen
import cortex.api.auth.AuthDTO.*
import cortex.api.syntax.Routes
import cortex.api.SimpleRoute
import cortex.application.auth.{loginUser, registerUser, LoginError, PasswordHasher, RegisterError}
import cortex.domain.auth.{AccessTokenService, RefreshTokenService, TokenPair}
import cortex.domain.users.{User, UserRepository}
import io.circe.generic.auto.*
import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.server.ServerEndpoint
import cortex.api.syntax.*
import cortex.application.auth
import cats.implicits.toFunctorOps
import cats.syntax.either.*

final class AuthRoutes[F[_]: {Monad, Clock, UUIDGen}](
  userRepository: UserRepository[F],
  passwordHasher: PasswordHasher[F],
  accessTokenService: AccessTokenService[F],
  refreshTokenService: RefreshTokenService[F],
):
  val r: Routes[F] = registerR ~> loginUserR

  private lazy val registerR: SimpleRoute[F] =
    val ep = endpoint.post
      .in("api" / "auth" / "register")
      .in(jsonBody[Credentials])
      .out(statusCode(StatusCode.Created))
      .out(jsonBody[SignUpResponse])
      .errorOut(statusCode.and(jsonBody[ErrorResponse]))
      .summary("Sign up")

    ep.serverLogic[F] { request =>
      registerUser(userRepository, passwordHasher, request.email, request.password).map {
        (result: Either[RegisterError, User]) =>
          result
            .leftMap(toRegisterError)
            .map((user: User) => SignUpResponse(user.id.value.toString, user.email.value))
      }
    }

  private lazy val loginUserR: SimpleRoute[F] =
    val ep = endpoint.post
      .in("api" / "auth" / "login")
      .in(jsonBody[Credentials])
      .out(statusCode(StatusCode.Accepted))
      .out(jsonBody[LoginResponse])
      .errorOut(statusCode.and(jsonBody[ErrorResponse]))
      .summary("Login")

    ep.serverLogic[F] { request =>
      loginUser(
        userRepository,
        passwordHasher,
        accessTokenService,
        refreshTokenService,
        request.email,
        request.password
      ).map { (result: Either[LoginError, TokenPair]) =>
        result
          .leftMap(toLoginError)
          .map { (tokens: TokenPair) =>
            LoginResponse(tokens.accessToken.value, tokens.refreshToken.value)
          }
      }
    }

  private def toRegisterError(error: RegisterError): (StatusCode, ErrorResponse) =
    error match
      case RegisterError.InvalidEmail(message)    =>
        (StatusCode.BadRequest, ErrorResponse("invalid_email", message))
      case RegisterError.InvalidPassword(message) =>
        (StatusCode.BadRequest, ErrorResponse("invalid_password", message))
      case RegisterError.EmailAlreadyExists(_)    =>
        (StatusCode.Conflict, ErrorResponse("email_already_exists", "Email is already registered"))

  private def toLoginError(error: LoginError): (StatusCode, ErrorResponse) =
    error match
      case LoginError.InvalidEmail(message)    => (StatusCode.BadRequest, ErrorResponse("invalid_email", message))
      case LoginError.InvalidPassword(message) => (StatusCode.BadRequest, ErrorResponse("invalid_password", message))
      case LoginError.InvalidCredentials       =>
        (StatusCode.Unauthorized, ErrorResponse("invalid_credentials", "Invalid credentials"))
