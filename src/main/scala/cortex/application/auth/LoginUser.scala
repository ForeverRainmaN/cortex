package cortex.application.auth

import cats.data.EitherT
import cats.effect.kernel.Async
import cats.implicits.*
import cortex.domain.users.{Email, User, UserRepository}

def loginUser[F[_]: Async](
  userRepository: UserRepository[F],
  passwordHasher: PasswordHasher[F],
  email: String,
  rawPassword: String
): F[Either[LoginError, User]] =
  (for
    validEmail <- EitherT.fromEither[F](Email.fromString(email).leftMap(LoginError.InvalidEmail(_)))
    _          <- EitherT.fromEither[F](validatePassword(rawPassword).leftMap(LoginError.InvalidPassword(_)))
    user       <- EitherT.fromOptionF(
                    userRepository.findByEmail(validEmail),
                    LoginError.InvalidCredentials: LoginError
                  )
    isValid    <- EitherT.liftF(passwordHasher.verify(rawPassword, user.hashedPassword))
    _          <- EitherT.cond[F](
                    isValid,
                    (),
                    LoginError.InvalidCredentials: LoginError
                  )
  yield user).value
