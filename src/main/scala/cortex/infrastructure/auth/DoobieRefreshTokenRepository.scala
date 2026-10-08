package cortex.infrastructure.auth

import cats.effect.kernel.Async
import cortex.domain.auth.{RefreshTokenHash, RefreshTokenId, RefreshTokenRecord, RefreshTokenRepository}
import cats.implicits.*
import doobie.implicits.*
import doobie.postgres.implicits.*
import cortex.infrastructure.persistence.DoobieMappings.given
import doobie.{ConnectionIO, Transactor}

import java.time.Instant

final private[infrastructure] class DoobieRefreshTokenRepository[F[_]: Async] private (xa: Transactor[F]) extends RefreshTokenRepository[F]:
  override def create(token: RefreshTokenRecord): F[Unit] =
    insert(token).transact(xa).void

  override def findActiveByHash(tokenHash: RefreshTokenHash, now: Instant): F[Option[RefreshTokenRecord]] =
    sql"""SELECT id, user_id, token_hash, expires_at, created_at, revoked_at
            FROM refresh_tokens
            WHERE token_hash = $tokenHash
              AND revoked_at IS NULL
              AND expires_at > $now
         """.query[RefreshTokenRecord].option.transact(xa)

  override def revoke(id: RefreshTokenId, revokedAt: Instant): F[Boolean] =
    sql"""UPDATE refresh_tokens
            SET revoked_at = $revokedAt
            WHERE id = $id
              AND revoked_at IS NULL
         """.update.run.transact(xa).map(_ > 0)

  override def rotate(
    oldHash: RefreshTokenHash,
    replacement: RefreshTokenRecord,
    now: Instant
  ): F[Boolean] =
    val revokeOld: ConnectionIO[Int] =
      sql"""UPDATE refresh_tokens
              SET revoked_at = $now
              WHERE token_hash = $oldHash
                AND revoked_at IS NULL
                AND expires_at > $now
           """.update.run

    val rotation: ConnectionIO[Boolean] =
      revokeOld.flatMap: updatedRows =>
        if updatedRows == 1 then insert(replacement).as(true)
        else false.pure[ConnectionIO]

    rotation.transact(xa)

  private def insert(token: RefreshTokenRecord): ConnectionIO[Int] =
    sql"""INSERT INTO refresh_tokens(
            id, user_id, token_hash, expires_at, created_at, revoked_at
          ) VALUES (
            ${token.id},
            ${token.userId},
            ${token.tokenHash},
            ${token.expiresAt},
            ${token.createdAt},
            ${token.revokedAt}
          )""".update.run

object DoobieRefreshTokenRepository:
  def apply[F[_]: Async](xa: Transactor[F]): RefreshTokenRepository[F] =
    new DoobieRefreshTokenRepository[F](xa)
