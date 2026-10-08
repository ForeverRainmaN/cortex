package cortex.infrastructure.auth

import cats.data.EitherT
import cats.effect.kernel.{Async, Clock}
import cats.effect.std.UUIDGen
import cortex.domain.auth.{
  RefreshToken,
  RefreshTokenError,
  RefreshTokenGenerator,
  RefreshTokenHasher,
  RefreshTokenId,
  RefreshTokenRecord,
  RefreshTokenRepository,
  RefreshTokenService,
  RefreshTokenRotation
}
import cortex.domain.users.UserId
import cats.implicits.*

import java.time.Instant
import scala.concurrent.duration.FiniteDuration

private type RefreshTokenErrorOr[A] = Either[RefreshTokenError, A]

final private case class GeneratedRefreshToken(
  record: RefreshTokenRecord,
  value: RefreshToken
)

final private[infrastructure] class LiveRefreshTokenService[F[_]: {Async, Clock}] private (
  repository: RefreshTokenRepository[F],
  refreshTokenGenerator: RefreshTokenGenerator[F],
  refreshTokenHasher: RefreshTokenHasher[F],
  refreshTokenTtl: FiniteDuration,
) extends RefreshTokenService[F]:

  override def issue(userId: UserId): F[RefreshToken] =
    for
      timestamp <- Clock[F].realTimeInstant
      expiresAt  = timestamp.plusMillis(refreshTokenTtl.toMillis)
      generated <- generateRefreshToken(userId, timestamp, expiresAt)
      _         <- repository.create(generated.record)
    yield generated.value

  override def rotate(
    token: RefreshToken
  ): F[RefreshTokenErrorOr[RefreshTokenRotation]] =
    val rotation = for
      timestamp      <- EitherT.liftF(Clock[F].realTimeInstant)
      oldTokenRecord <- EitherT.fromOptionF(
                          findOldTokenRecord(token, timestamp),
                          RefreshTokenError.InvalidOrExpired
                        )
      newExpiresAt    = timestamp.plusMillis(refreshTokenTtl.toMillis)
      generated      <- EitherT.liftF(generateRefreshToken(oldTokenRecord.userId, timestamp, newExpiresAt))
      isRotated      <- EitherT.liftF(repository.rotate(oldTokenRecord.tokenHash, generated.record, timestamp))
      _              <- EitherT.cond[F](
                          isRotated,
                          (),
                          RefreshTokenError.InvalidOrExpired
                        )
    yield RefreshTokenRotation(
      userId = oldTokenRecord.userId,
      refreshToken = generated.value
    )

    rotation.value

  override def revoke(token: RefreshToken): F[Boolean] =
    for
      timestamp <- Clock[F].realTimeInstant
      record    <- findOldTokenRecord(token, timestamp)
      result    <- record.fold(false.pure[F]): record =>
                     repository.revoke(record.id, timestamp)
    yield result

  private def generateRefreshToken(
    userId: UserId,
    createdAt: Instant,
    expiresAt: Instant
  ): F[GeneratedRefreshToken] =
    for
      value     <- refreshTokenGenerator.generate
      id        <- UUIDGen.randomUUID[F].map(RefreshTokenId(_))
      tokenHash <- refreshTokenHasher.hash(value)
    yield GeneratedRefreshToken(
      record = RefreshTokenRecord(
        id = id,
        userId = userId,
        tokenHash = tokenHash,
        createdAt = createdAt,
        expiresAt = expiresAt,
        revokedAt = None
      ),
      value = value
    )

  private def findOldTokenRecord(token: RefreshToken, timestamp: Instant): F[Option[RefreshTokenRecord]] =
    for
      oldTokenHash        <- refreshTokenHasher.hash(token)
      maybeOldTokenRecord <- repository.findActiveByHash(oldTokenHash, timestamp)
    yield maybeOldTokenRecord
