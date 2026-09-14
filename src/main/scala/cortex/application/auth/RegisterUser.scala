package cortex.application.auth

import cats.data.EitherT
import cats.effect.Clock
import cats.effect.kernel.Async
import cats.effect.std.UUIDGen
import cortex.domain.users.{Email, HashedPassword, User, UserId, UserRepository}
import cats.implicits.*

def registerUser[F[_]: Async](
  userRepository: UserRepository[F],
  passwordHasher: PasswordHasher[F],
  email: String,
  rawPassword: String
): F[Either[RegisterError, User]] =
  (for
    validEmail <- EitherT.fromEither[F](Email.fromString(email).leftMap(RegisterError.InvalidEmail(_)))
    _          <- EitherT.fromEither[F](validatePassword(rawPassword).leftMap(RegisterError.InvalidPassword(_)))
    existing   <- EitherT.liftF(userRepository.findByEmail(validEmail))
    _          <- EitherT.cond[F](existing.isEmpty, (), RegisterError.EmailAlreadyExists(validEmail))
    hash       <- EitherT.liftF(passwordHasher.hash(rawPassword))
    timestamp  <- EitherT.liftF(Clock[F].realTimeInstant)
    id         <- EitherT.liftF(UUIDGen.randomUUID[F].map(UserId(_)))
    user       <- EitherT.liftF(userRepository.create(User.make(id, validEmail, hash, timestamp)))
  yield user).value
