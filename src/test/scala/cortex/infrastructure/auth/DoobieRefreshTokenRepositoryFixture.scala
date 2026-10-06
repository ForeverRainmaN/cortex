package cortex.infrastructure.auth

import cats.effect.IO
import cortex.domain.auth.{RefreshTokenHash, RefreshTokenRecord}
import cortex.domain.users.UserId
import cortex.infrastructure.users.DoobieSpec
import doobie.Transactor
import doobie.implicits.*
import doobie.postgres.implicits.*

import java.time.Instant
import java.util.UUID

trait DoobieRefreshTokenRepositoryFixture:
  self: DoobieSpec =>

  private val createdAt = Instant.parse("2026-10-06T19:00:00Z")
  private val expiresAt = Instant.parse("2026-11-06T19:00:00Z")

  protected val testId: UUID =
    UUID.fromString("00000000-0000-0000-0000-000000000001")

  protected val testUserId: UserId =
    UserId.generate

  protected val testTokenHash: RefreshTokenHash =
    RefreshTokenHash("test hash")

  protected val testRecord: RefreshTokenRecord =
    RefreshTokenRecord(
      id = testId,
      userId = testUserId,
      tokenHash = testTokenHash,
      expiresAt = expiresAt,
      createdAt = createdAt,
      revokedAt = None
    )

  protected def insertUser(xa: Transactor[IO]): IO[Unit] =
    sql"""
      INSERT INTO users (user_id)
      VALUES (${testRecord.userId.value})
    """.update.run
      .transact(xa)
      .void

  protected def withRepositoryAndUser[R, A](
    makeRepository: Transactor[IO] => IO[R]
  )(test: R => IO[A]): IO[A] =
    transactor.use: xa =>
      for
        _          <- insertUser(xa)
        repository <- makeRepository(xa)
        result     <- test(repository)
      yield result
