package cortex.infrastructure.auth

import cats.effect.IO
import cortex.domain.auth.{RefreshTokenHash, RefreshTokenId, RefreshTokenRecord, RefreshTokenRepository}
import cortex.domain.users.UserId
import cortex.infrastructure.persistence.DoobieSpec
import cortex.infrastructure.persistence.DoobieMappings.given
import doobie.{ConnectionIO, Transactor}
import doobie.implicits.*
import doobie.postgres.implicits.*

import java.time.Instant
import java.util.UUID

trait DoobieRefreshTokenRepositoryFixture:
  self: DoobieSpec =>

  protected val referenceTime: Instant = Instant.parse("2026-10-07T12:00:00Z")
  protected val createdAt: Instant     = Instant.parse("2026-10-06T19:00:00Z")
  protected val expiresAt: Instant     = Instant.parse("2026-11-06T19:00:00Z")

  protected val userId: UserId = UserId.generate

  protected val refreshTokenId: RefreshTokenId = RefreshTokenId(
    UUID.fromString("00000000-0000-0000-0000-000000000001")
  )

  protected val tokenHash: RefreshTokenHash =
    RefreshTokenHash("test-hash-123")

  protected val refreshTokenRecord: RefreshTokenRecord =
    RefreshTokenRecord(
      id = refreshTokenId,
      userId = userId,
      tokenHash = tokenHash,
      expiresAt = expiresAt,
      createdAt = createdAt,
      revokedAt = None
    )

  protected def insertUser(xa: Transactor[IO]): IO[Unit] =
    sql"""
      INSERT INTO users (
        user_id,
        email,
        hashed_password,
        created_at
      ) VALUES (
        ${userId.value},
        'refresh-token-test@example.com',
        'test-hash',
        $createdAt
      )
    """.update.run
      .transact(xa)
      .void

  protected def insertRefreshTokenRecord(token: RefreshTokenRecord): ConnectionIO[Int] =
    sql"""
         INSERT INTO refresh_tokens (
          id,
          user_id,
          token_hash,
          expires_at,
          created_at,
          revoked_at
         ) VALUES (
            ${token.id.value},
            ${token.userId.value},
            ${token.tokenHash},
            ${token.expiresAt},
            ${token.createdAt},
            ${token.revokedAt}
         )
         """.update.run

  protected def readRefreshTokenRecord(id: RefreshTokenId): ConnectionIO[Option[RefreshTokenRecord]] =
    sql"""SELECT id, user_id, token_hash, expires_at, created_at, revoked_at
          FROM refresh_tokens
          WHERE id = $id
       """.query[RefreshTokenRecord].option

  protected def withRefreshTokenRepository[A](
    test: (RefreshTokenRepository[IO], Transactor[IO]) => IO[A]
  ): IO[A] =
    transactor.use: xa =>
      for
        _          <- insertUser(xa)
        repository <- DoobieRefreshTokenRepository[IO](xa)
        result     <- test(repository, xa)
      yield result

  protected def withToken[A](record: RefreshTokenRecord)(
    test: (RefreshTokenRepository[IO], Transactor[IO]) => IO[A]
  ): IO[A] =
    withRefreshTokenRepository: (repository, xa) =>
      for
        _      <- insertRefreshTokenRecord(record).transact(xa)
        result <- test(repository, xa)
      yield result
