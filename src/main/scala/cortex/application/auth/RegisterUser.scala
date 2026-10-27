package cortex.application.auth

import cats.Monad
import cats.data.EitherT
import cats.effect.Clock
import cats.effect.std.UUIDGen
import cats.implicits.*
import cortex.domain.users.{CreateUserError, Email, HashedPassword, User, UserId, UserRepository}
import cortex.syntax.*

def registerUser[F[_]: {Monad, Clock, UUIDGen}](
  userRepository: UserRepository[F],
  passwordHasher: PasswordHasher[F],
  email: String,
  rawPassword: String
): F[Either[RegisterError, User]] =
  (for
    validEmail <- EitherT.fromEither[F](Email.fromString(email).leftMap(RegisterError.InvalidEmail(_)))
    _          <- EitherT.fromEither[F](validatePassword(rawPassword).leftMap(RegisterError.InvalidPassword(_)))
    existing   <- EitherT.liftF(userRepository.findByEmail(validEmail))
    _          <- EitherT.condF[F, RegisterError](existing.isEmpty, RegisterError.EmailAlreadyExists(validEmail))
    hash       <- EitherT.liftF(passwordHasher.hash(rawPassword))
    timestamp  <- EitherT.liftF(Clock[F].realTimeInstant)
    id         <- EitherT.liftF(UUIDGen.randomUUID[F].map(UserId(_)))
    user       <- EitherT(userRepository.create(User.make(id, validEmail, hash, timestamp))).leftMap {
                    case CreateUserError.EmailAlreadyExists(email) => RegisterError.EmailAlreadyExists(email)
                  }
  yield user).value
