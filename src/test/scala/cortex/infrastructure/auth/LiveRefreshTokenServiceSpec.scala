package cortex.infrastructure.auth

import cats.effect.{IO, Ref}
import cats.effect.testing.scalatest.AsyncIOSpec
import cortex.domain.auth.{RefreshToken, RefreshTokenError, RefreshTokenHash, RefreshTokenRecord, RefreshTokenRotation}
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers

import java.time.Instant
import scala.concurrent.duration.*

class LiveRefreshTokenServiceSpec extends AsyncFlatSpec, AsyncIOSpec, Matchers, LiveRefreshTokenServiceFixture:

  it should "issue a token and store its hash with createdAt = now and expiresAt = now + ttl" in:
    withService: fixture =>
      for
        token   <- fixture.service.issue(userId)
        records <- fixture.repository.all
      yield
        token shouldBe generatedToken(1)
        records should have size 1
        val record = records.head
        record.userId shouldBe userId
        record.tokenHash shouldBe hashOf(token)
        record.createdAt shouldBe referenceTime
        record.expiresAt shouldBe expiresAtFor(referenceTime)
        record.revokedAt shouldBe None

  it should "never store the raw token value" in:
    withService: fixture =>
      for
        token   <- fixture.service.issue(userId)
        records <- fixture.repository.all
      yield records.map(_.tokenHash.value) should not contain token.value

  it should "issue distinct tokens with distinct ids" in:
    withService: fixture =>
      for
        first   <- fixture.service.issue(userId)
        second  <- fixture.service.issue(userId)
        records <- fixture.repository.all
      yield
        first should not be second
        records.map(_.id).distinct should have size 2

  it should "propagate repository failures from issue through IO" in:
    val failure    = new RuntimeException("Database unavailable")
    val repository = new StubRepository:
      override def create(token: RefreshTokenRecord): IO[Unit] = IO.raiseError(failure)

    withStubService(repository): service =>
      service.issue(userId).attempt.map(_ shouldBe Left(failure))

  it should "rotate an active token: revoke the old one and store a replacement for the same user" in:
    withService: fixture =>
      for
        oldToken  <- fixture.service.issue(userId)
        _         <- fixture.clock.advance(10.minutes)
        rotatedAt <- fixture.clock.realTimeInstant
        result    <- fixture.service.rotate(oldToken)
        records   <- fixture.repository.all
        newToken   = generatedToken(2)
        oldRecord  = records.find(_.tokenHash == hashOf(oldToken))
        newRecord  = records.find(_.tokenHash == hashOf(newToken))
      yield
        result shouldBe Right(RefreshTokenRotation(userId, newToken))
        oldRecord.flatMap(_.revokedAt) shouldBe Some(rotatedAt)
        newRecord.map(_.userId) shouldBe Some(userId)
        newRecord.map(_.createdAt) shouldBe Some(rotatedAt)
        newRecord.map(_.expiresAt) shouldBe Some(expiresAtFor(rotatedAt))
        newRecord.flatMap(_.revokedAt) shouldBe None

  it should "reject an old token once it has been rotated" in:
    withService: fixture =>
      for
        oldToken <- fixture.service.issue(userId)
        _        <- fixture.service.rotate(oldToken)
        reused   <- fixture.service.rotate(oldToken)
        records  <- fixture.repository.all
      yield
        reused shouldBe Left(RefreshTokenError.InvalidOrExpired)
        records should have size 2

  it should "allow rotating the replacement token" in:
    withService: fixture =>
      for
        first  <- fixture.service.issue(userId)
        second <- fixture.service.rotate(first)
        third  <- second.fold(
                    error => IO.raiseError(new AssertionError(s"Unexpected rotation error: $error")),
                    rotation => fixture.service.rotate(rotation.refreshToken)
                  )
      yield third shouldBe Right(RefreshTokenRotation(userId, generatedToken(3)))

  it should "return InvalidOrExpired for an unknown token and store nothing" in:
    withService: fixture =>
      for
        result  <- fixture.service.rotate(RefreshToken.fromString("unknown"))
        records <- fixture.repository.all
      yield
        result shouldBe Left(RefreshTokenError.InvalidOrExpired)
        records shouldBe empty

  it should "return InvalidOrExpired for an expired token" in:
    withService: fixture =>
      for
        token  <- fixture.service.issue(userId)
        _      <- fixture.clock.advance(ttl + 1.second)
        result <- fixture.service.rotate(token)
      yield result shouldBe Left(RefreshTokenError.InvalidOrExpired)

  it should "treat a token as expired exactly at expiresAt" in:
    withService: fixture =>
      for
        token  <- fixture.service.issue(userId)
        _      <- fixture.clock.advance(ttl)
        result <- fixture.service.rotate(token)
      yield result shouldBe Left(RefreshTokenError.InvalidOrExpired)

  it should "accept a token right before it expires" in:
    withService: fixture =>
      for
        token  <- fixture.service.issue(userId)
        _      <- fixture.clock.advance(ttl - 1.millisecond)
        result <- fixture.service.rotate(token)
      yield result.map(_.userId) shouldBe Right(userId)

  it should "return InvalidOrExpired for a revoked token" in:
    withService: fixture =>
      for
        token  <- fixture.service.issue(userId)
        _      <- fixture.service.revoke(token)
        result <- fixture.service.rotate(token)
      yield result shouldBe Left(RefreshTokenError.InvalidOrExpired)

  it should "return InvalidOrExpired when the repository rejects the rotation (lost race)" in:
    val token      = RefreshToken.fromString("raced")
    val repository = new StubRepository:
      override def findActiveByHash(tokenHash: RefreshTokenHash, now: Instant): IO[Option[RefreshTokenRecord]] =
        IO.pure(Some(activeRecord(token)))
      override def rotate(oldHash: RefreshTokenHash, replacement: RefreshTokenRecord, now: Instant): IO[Boolean] =
        IO.pure(false)

    withStubService(repository): service =>
      service.rotate(token).map(_ shouldBe Left(RefreshTokenError.InvalidOrExpired))

  it should "pass the old hash and the current time to the repository on rotation" in:
    val token = RefreshToken.fromString("old")
    for
      calls      <- Ref.of[IO, List[(RefreshTokenHash, RefreshTokenRecord, Instant)]](Nil)
      repository  = new StubRepository:
                      override def findActiveByHash(
                        tokenHash: RefreshTokenHash,
                        now: Instant
                      ): IO[Option[RefreshTokenRecord]] =
                        IO.pure(Some(activeRecord(token)))
                      override def rotate(
                        oldHash: RefreshTokenHash,
                        replacement: RefreshTokenRecord,
                        now: Instant
                      ): IO[Boolean] =
                        calls.update(_ :+ ((oldHash, replacement, now))).as(true)
      _          <- withStubService(repository)(_.rotate(token).map(_ shouldBe a[Right[?, ?]]))
      recorded   <- calls.get
    yield
      recorded should have size 1
      val (oldHash, replacement, now) = recorded.head
      oldHash shouldBe hashOf(token)
      now shouldBe referenceTime
      replacement.tokenHash shouldBe hashOf(generatedToken(1))
      replacement.createdAt shouldBe referenceTime

  it should "propagate repository failures from rotate through IO" in:
    val failure    = new RuntimeException("Database unavailable")
    val repository = new StubRepository:
      override def findActiveByHash(tokenHash: RefreshTokenHash, now: Instant): IO[Option[RefreshTokenRecord]] =
        IO.raiseError(failure)

    withStubService(repository): service =>
      service.rotate(RefreshToken.fromString("any")).attempt.map(_ shouldBe Left(failure))

  it should "revoke an active token at the current time" in:
    withService: fixture =>
      for
        token     <- fixture.service.issue(userId)
        _         <- fixture.clock.advance(5.minutes)
        revokedAt <- fixture.clock.realTimeInstant
        revoked   <- fixture.service.revoke(token)
        records   <- fixture.repository.all
      yield
        revoked shouldBe true
        records.map(_.revokedAt) shouldBe Vector(Some(revokedAt))

  it should "return false when revoking a token twice" in:
    withService: fixture =>
      for
        token  <- fixture.service.issue(userId)
        first  <- fixture.service.revoke(token)
        second <- fixture.service.revoke(token)
      yield
        first shouldBe true
        second shouldBe false

  it should "return false for an unknown token without calling repository.revoke" in:
    val repository = new StubRepository:
      override def findActiveByHash(tokenHash: RefreshTokenHash, now: Instant): IO[Option[RefreshTokenRecord]] =
        IO.pure(None)

    withStubService(repository): service =>
      service.revoke(RefreshToken.fromString("unknown")).map(_ shouldBe false)

  it should "return false for an expired token" in:
    withService: fixture =>
      for
        token   <- fixture.service.issue(userId)
        _       <- fixture.clock.advance(ttl)
        revoked <- fixture.service.revoke(token)
        records <- fixture.repository.all
      yield
        revoked shouldBe false
        records.map(_.revokedAt) shouldBe Vector(None)

  it should "not affect other tokens of the same user" in:
    withService: fixture =>
      for
        first  <- fixture.service.issue(userId)
        second <- fixture.service.issue(userId)
        _      <- fixture.service.revoke(first)
        result <- fixture.service.rotate(second)
      yield result.map(_.userId) shouldBe Right(userId)
