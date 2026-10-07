package cortex.infrastructure.auth

import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import cortex.domain.auth.{RefreshTokenHash, RefreshTokenId}
import cortex.infrastructure.persistence.DoobieSpec
import doobie.implicits.*
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
      tokens
        .revoke(RefreshTokenId.generate, referenceTime).map: revoked =>
          revoked shouldBe false

  it should "successfully rotate old token and add a replacement" in:
    val replacementRecord =
      refreshTokenRecord.copy(
        id = RefreshTokenId.generate,
        createdAt = referenceTime,
        tokenHash = RefreshTokenHash("new-hash")
      )

    withToken(refreshTokenRecord): (tokens, xa) =>
      for
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
    val replacementRecord =
      refreshTokenRecord.copy(
        id = RefreshTokenId.generate,
        createdAt = referenceTime,
        tokenHash = RefreshTokenHash("new-hash")
      )

    withRefreshTokenRepository: (tokens, xa) =>
      for
        rotated           <- tokens.rotate(refreshTokenRecord.tokenHash, replacementRecord, referenceTime)
        replacementFromDb <- readRefreshTokenRecord(replacementRecord.id).transact(xa)
      yield
        rotated shouldBe false
        replacementFromDb shouldBe None

  it should "return false when trying to rotate a record that has an expired refresh token" in:
    val expiredTokenRecord = refreshTokenRecord.copy(expiresAt = referenceTime.minusSeconds(60))
    val replacementRecord  =
      refreshTokenRecord.copy(
        id = RefreshTokenId.generate,
        createdAt = referenceTime,
        tokenHash = RefreshTokenHash("new-hash")
      )
    withToken(expiredTokenRecord): (tokens, xa) =>
      for
        rotated           <- tokens.rotate(expiredTokenRecord.tokenHash, replacementRecord, referenceTime)
        oldRecord         <- readRefreshTokenRecord(expiredTokenRecord.id).transact(xa)
        replacementFromDb <- readRefreshTokenRecord(replacementRecord.id).transact(xa)
      yield
        rotated shouldBe false
        oldRecord shouldBe Some(expiredTokenRecord)
        replacementFromDb shouldBe None

  it should "return false when trying to rotate a record that has a revoked refresh token" in:
    val replacementRecord =
      refreshTokenRecord.copy(
        id = RefreshTokenId.generate,
        createdAt = referenceTime,
        tokenHash = RefreshTokenHash("new-hash")
      )
    val revokedTokenRecord = refreshTokenRecord.copy(revokedAt = Some(referenceTime))

    withToken(revokedTokenRecord): (tokens, xa) =>
      for
        rotated           <- tokens.rotate(revokedTokenRecord.tokenHash, replacementRecord, referenceTime)
        oldRecord         <- readRefreshTokenRecord(revokedTokenRecord.id).transact(xa)
        replacementFromDb <- readRefreshTokenRecord(replacementRecord.id).transact(xa)
      yield
        rotated shouldBe false
        oldRecord shouldBe Some(revokedTokenRecord)
        replacementFromDb shouldBe None

  /*
  5. Ошибка вставки replacement: транзакция откатывается, старый остаётся активным.
  6. Две параллельные ротации: ровно одна успешна, сохранён только её replacement.
   */
