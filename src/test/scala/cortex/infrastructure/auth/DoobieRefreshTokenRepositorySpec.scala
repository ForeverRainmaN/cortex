package cortex.infrastructure.auth

import cats.effect.IO
import cats.effect.std.UUIDGen
import cats.effect.testing.scalatest.AsyncIOSpec
import cats.implicits.*
import cortex.domain.auth.{RefreshTokenHash, RefreshTokenId}
import cortex.infrastructure.persistence.DoobieSpec
import doobie.implicits.*
import org.postgresql.util.PSQLException
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers
class DoobieRefreshTokenRepositorySpec
  extends AsyncFlatSpec, AsyncIOSpec, DoobieSpec, Matchers, DoobieRefreshTokenRepositoryFixture:

  it should "create a new refresh token" in:
    withRefreshTokenRepository: (tokens, xa) =>
      for
        _          <- tokens.create(refreshTokenRecord)
        maybeToken <- readRefreshTokenRecord(refreshTokenRecord.id).transact(xa)
      yield maybeToken shouldBe Some(refreshTokenRecord)

  it should "find active refresh token by hash" in:
    withToken(refreshTokenRecord): (tokens, _) =>
      tokens
        .findActiveByHash(tokenHash, referenceTime).map: maybeToken =>
          maybeToken shouldBe Some(refreshTokenRecord)

  it should "return None when the refresh token does not exist" in:
    withRepository(DoobieRefreshTokenRepository[IO](_)): tokens =>
      tokens
        .findActiveByHash(tokenHash, referenceTime).map: maybeToken =>
          maybeToken shouldBe None

  it should "return None for a revoked refresh token" in:
    val revokedTokenRecord = refreshTokenRecord.copy(revokedAt = Some(referenceTime))

    withToken(revokedTokenRecord): (tokens, _) =>
      tokens
        .findActiveByHash(tokenHash, referenceTime).map: maybeToken =>
          maybeToken shouldBe None

  it should "return None for an expired refresh token" in:
    val expiredTokenRecord = refreshTokenRecord.copy(expiresAt = referenceTime.minusSeconds(1))

    withToken(expiredTokenRecord): (tokens, _) =>
      tokens
        .findActiveByHash(tokenHash, referenceTime).map: maybeToken =>
          maybeToken shouldBe None

  it should "return None when expiresAt equals the reference time" in:
    val expiringTokenRecord = refreshTokenRecord.copy(expiresAt = referenceTime)

    withToken(expiringTokenRecord): (tokens, _) =>
      tokens
        .findActiveByHash(tokenHash, referenceTime).map: maybeToken =>
          maybeToken shouldBe None

  it should "revoke an active token and persist the supplied revokedAt" in:
    withToken(refreshTokenRecord): (tokens, xa) =>
      for
        revoked    <- tokens.revoke(refreshTokenRecord.id, referenceTime)
        maybeToken <- readRefreshTokenRecord(refreshTokenRecord.id).transact(xa)
      yield
        revoked shouldBe true
        maybeToken shouldBe Some(refreshTokenRecord.copy(revokedAt = Some(referenceTime)))

  it should "return false on repeated revocation and preserve the original revokedAt" in:
    val secondRevocationTime = referenceTime.plusSeconds(60)

    withToken(refreshTokenRecord): (tokens, xa) =>
      for
        revoked       <- tokens.revoke(refreshTokenRecord.id, referenceTime)
        revokedSecond <- tokens.revoke(refreshTokenRecord.id, secondRevocationTime)
        maybeToken    <- readRefreshTokenRecord(refreshTokenRecord.id).transact(xa)
      yield
        revoked shouldBe true
        revokedSecond shouldBe false
        maybeToken shouldBe Some(refreshTokenRecord.copy(revokedAt = Some(referenceTime)))

  it should "return false if the token id is unknown" in:
    withRepository(DoobieRefreshTokenRepository[IO](_)): tokens =>
      for
        unknownId <- UUIDGen.randomUUID[IO].map(RefreshTokenId(_))
        revoked   <- tokens.revoke(unknownId, referenceTime)
      yield revoked shouldBe false

  it should "successfully rotate old token and add a replacement" in:
    withToken(refreshTokenRecord): (tokens, xa) =>
      for
        replacementId     <- UUIDGen.randomUUID[IO].map(RefreshTokenId(_))
        replacementRecord  = refreshTokenRecord.copy(
                               id = replacementId,
                               createdAt = referenceTime,
                               tokenHash = RefreshTokenHash("new-hash")
                             )
        rotated           <- tokens.rotate(
                               refreshTokenRecord.tokenHash,
                               replacementRecord,
                               referenceTime
                             )
        oldRecord         <- readRefreshTokenRecord(refreshTokenRecord.id).transact(xa)
        replacementFromDb <- readRefreshTokenRecord(replacementRecord.id).transact(xa)
      yield
        rotated shouldBe true
        oldRecord shouldBe Some(refreshTokenRecord.copy(revokedAt = Some(referenceTime)))
        replacementFromDb shouldBe Some(replacementRecord)

  it should "return false when trying to rotate old record that does not exist" in:
    withRefreshTokenRepository: (tokens, xa) =>
      for
        replacementId     <- UUIDGen.randomUUID[IO].map(RefreshTokenId(_))
        replacementRecord  = refreshTokenRecord.copy(
                               id = replacementId,
                               createdAt = referenceTime,
                               tokenHash = RefreshTokenHash("new-hash")
                             )
        rotated           <- tokens.rotate(refreshTokenRecord.tokenHash, replacementRecord, referenceTime)
        replacementFromDb <- readRefreshTokenRecord(replacementRecord.id).transact(xa)
      yield
        rotated shouldBe false
        replacementFromDb shouldBe None

  it should "return false when trying to rotate a record that has an expired refresh token" in:
    val expiredTokenRecord = refreshTokenRecord.copy(expiresAt = referenceTime.minusSeconds(60))

    withToken(expiredTokenRecord): (tokens, xa) =>
      for
        replacementId     <- UUIDGen.randomUUID[IO].map(RefreshTokenId(_))
        replacementRecord  = refreshTokenRecord.copy(
                               id = replacementId,
                               createdAt = referenceTime,
                               tokenHash = RefreshTokenHash("new-hash")
                             )
        rotated           <- tokens.rotate(expiredTokenRecord.tokenHash, replacementRecord, referenceTime)
        oldRecord         <- readRefreshTokenRecord(expiredTokenRecord.id).transact(xa)
        replacementFromDb <- readRefreshTokenRecord(replacementRecord.id).transact(xa)
      yield
        rotated shouldBe false
        oldRecord shouldBe Some(expiredTokenRecord)
        replacementFromDb shouldBe None

  it should "return false when trying to rotate a record that has a revoked refresh token" in:
    val revokedTokenRecord = refreshTokenRecord.copy(revokedAt = Some(referenceTime))

    withToken(revokedTokenRecord): (tokens, xa) =>
      for
        replacementId     <- UUIDGen.randomUUID[IO].map(RefreshTokenId(_))
        replacementRecord  = refreshTokenRecord.copy(
                               id = replacementId,
                               createdAt = referenceTime,
                               tokenHash = RefreshTokenHash("new-hash")
                             )
        rotated           <- tokens.rotate(revokedTokenRecord.tokenHash, replacementRecord, referenceTime)
        oldRecord         <- readRefreshTokenRecord(revokedTokenRecord.id).transact(xa)
        replacementFromDb <- readRefreshTokenRecord(replacementRecord.id).transact(xa)
      yield
        rotated shouldBe false
        oldRecord shouldBe Some(revokedTokenRecord)
        replacementFromDb shouldBe None

  it should "rollback revocation when replacement cannot be inserted" in:
    withToken(refreshTokenRecord): (tokens, xa) =>
      for
        replacementId <- UUIDGen.randomUUID[IO].map(RefreshTokenId(_))

        replacementRecord =
          refreshTokenRecord.copy(id = replacementId)

        result <- tokens
                    .rotate(
                      refreshTokenRecord.tokenHash,
                      replacementRecord,
                      referenceTime
                    )
                    .attempt

        oldRecord         <- readRefreshTokenRecord(refreshTokenRecord.id).transact(xa)
        replacementFromDb <- readRefreshTokenRecord(replacementRecord.id).transact(xa)
      yield
        result match
          case Left(error: PSQLException) =>
            error.getSQLState shouldBe "23505"
          case _                          =>
            fail("Expected replacement insertion to fail")

        oldRecord shouldBe Some(refreshTokenRecord)
        replacementFromDb shouldBe None

  it should "allow only one concurrent rotation" in:
    withToken(refreshTokenRecord): (tokens, xa) =>
      for
        firstId  <- UUIDGen.randomUUID[IO].map(RefreshTokenId(_))
        secondId <- UUIDGen.randomUUID[IO].map(RefreshTokenId(_))

        firstReplacement =
          refreshTokenRecord.copy(
            id = firstId,
            tokenHash = RefreshTokenHash("first-replacement"),
            createdAt = referenceTime
          )

        secondReplacement =
          refreshTokenRecord.copy(
            id = secondId,
            tokenHash = RefreshTokenHash("second-replacement"),
            createdAt = referenceTime
          )

        results <- (
                     tokens.rotate(
                       refreshTokenRecord.tokenHash,
                       firstReplacement,
                       referenceTime
                     ),
                     tokens.rotate(
                       refreshTokenRecord.tokenHash,
                       secondReplacement,
                       referenceTime
                     )
                   ).parTupled

        oldRecord    <- readRefreshTokenRecord(refreshTokenRecord.id).transact(xa)
        firstFromDb  <- readRefreshTokenRecord(firstReplacement.id).transact(xa)
        secondFromDb <- readRefreshTokenRecord(secondReplacement.id).transact(xa)
      yield
        results.count(identity) shouldBe 1
        oldRecord shouldBe Some(refreshTokenRecord.copy(revokedAt = Some(referenceTime)))
        List(firstFromDb, secondFromDb).flatten.size shouldBe 1
