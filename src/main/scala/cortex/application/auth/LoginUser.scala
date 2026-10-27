package cortex.application.auth

import cats.Monad
import cats.data.EitherT
import cats.implicits.*
import cortex.domain.auth.{AccessTokenService, RefreshTokenService, TokenPair}
import cortex.domain.users.{Email, User, UserRepository}
import cortex.syntax.*

def loginUser[F[_]: Monad](
  userRepository: UserRepository[F],
  passwordHasher: PasswordHasher[F],
  accessTokenService: AccessTokenService[F],
  refreshTokenService: RefreshTokenService[F],
  email: String,
  rawPassword: String
): F[Either[LoginError, TokenPair]] =
  (for
    validEmail   <- EitherT.fromEither[F](Email.fromString(email).leftMap(LoginError.InvalidEmail(_)))
    _            <- EitherT.fromEither[F](validatePassword(rawPassword).leftMap(LoginError.InvalidPassword(_)))
    user         <- EitherT.fromOptionF(
                      userRepository.findByEmail(validEmail),
                      LoginError.InvalidCredentials: LoginError
                    )
    isValid      <- EitherT.liftF(passwordHasher.verify(rawPassword, user.hashedPassword))
    _            <- EitherT.condF[F, LoginError](
                      isValid,
                      LoginError.InvalidCredentials: LoginError
                    )
    accessToken  <- EitherT.liftF(accessTokenService.issue(user.id))
    refreshToken <- EitherT.liftF(refreshTokenService.issue(user.id))
  yield TokenPair(accessToken, refreshToken)).value
