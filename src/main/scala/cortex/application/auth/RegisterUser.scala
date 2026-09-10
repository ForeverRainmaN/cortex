package cortex.application.auth

import cats.data.EitherT
import cats.effect.Clock
import cats.effect.kernel.Async
import cats.effect.std.UUIDGen
import cortex.domain.users.{Email, HashedPassword, User, UserId, UserRepository}
import cats.implicits.*
import tsec.passwordhashers.jca.BCrypt

import java.nio.charset.StandardCharsets.UTF_8

def registerUser[F[_]: Async](
  userRepository: UserRepository[F],
  email: String,
  rawPassword: String
): F[Either[RegisterError, User]] =
  val result: EitherT[F, RegisterError, User] = for
    validEmail <- EitherT.fromEither[F](Email.fromString(email).leftMap(RegisterError.InvalidEmail(_)))
    _          <- EitherT.cond[F](
                    rawPassword.nonEmpty && rawPassword.getBytes(UTF_8).length <= 72,
                    (),
                    RegisterError.InvalidPassword("Password must contain between 1 and 72 UTF-8 bytes")
                  )
    existing   <- EitherT.liftF(userRepository.findByEmail(validEmail))
    _          <- EitherT.cond[F](existing.isEmpty, (), RegisterError.EmailAlreadyExists(validEmail))
    hash       <- EitherT.liftF(BCrypt.hashpw[F](rawPassword.getBytes(UTF_8)))
    timestamp  <- EitherT.liftF(Clock[F].realTimeInstant)
    id         <- EitherT.liftF(UUIDGen.randomUUID[F].map(UserId(_)))
    user       <- EitherT.liftF(userRepository.create(User.make(id, validEmail, HashedPassword(hash), timestamp)))
  yield user
  result.value
