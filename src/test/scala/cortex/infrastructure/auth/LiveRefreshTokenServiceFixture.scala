package cortex.infrastructure.auth

import cats.effect.{Clock, IO, Ref}
import cortex.domain.auth.{
  RefreshToken,
  RefreshTokenGenerator,
  RefreshTokenHash,
  RefreshTokenHasher,
  RefreshTokenId,
  RefreshTokenRecord,
  RefreshTokenRepository,
  RefreshTokenService
}
import cortex.domain.users.UserId
import cortex.testkit.ManualClock

import java.time.Instant
import java.util.UUID
import scala.concurrent.duration.*

trait LiveRefreshTokenServiceFixture:
  protected val referenceTime: Instant = Instant.parse("2026-10-07T12:00:00Z")
  protected val ttl: FiniteDuration    = 30.days

  protected val userId: UserId = UserId.fromUUID(
    UUID.fromString("00000000-0000-0000-0000-000000000001")
  )

  protected val refreshTokenId: RefreshTokenId = RefreshTokenId.fromUUID(
    UUID.fromString("00000000-0000-0000-0000-000000000002")
  )

  protected def generatedToken(n: Int): RefreshToken =
    RefreshToken.fromString(s"token-$n")

  protected def hashOf(token: RefreshToken): RefreshTokenHash =
    RefreshTokenHash.fromHash(s"hash:${token.value}")

  protected def expiresAtFor(issuedAt: Instant): Instant =
    issuedAt.plusMillis(ttl.toMillis)

  protected def activeRecord(token: RefreshToken): RefreshTokenRecord =
    RefreshTokenRecord(
      id = refreshTokenId,
      userId = userId,
      tokenHash = hashOf(token),
      expiresAt = expiresAtFor(referenceTime),
      createdAt = referenceTime,
      revokedAt = None
    )

  protected val hasher: RefreshTokenHasher[IO] =
    token => IO.pure(hashOf(token))

  final protected class SequentialGenerator(counter: Ref[IO, Int]) extends RefreshTokenGenerator[IO]:
    override def generate: IO[RefreshToken] =
      counter.updateAndGet(_ + 1).map(generatedToken)

  final protected class InMemoryRepository(records: Ref[IO, Vector[RefreshTokenRecord]])
    extends RefreshTokenRepository[IO]:
    def all: IO[Vector[RefreshTokenRecord]] = records.get

    override def create(token: RefreshTokenRecord): IO[Unit] =
      records.update(_ :+ token)

    override def findActiveByHash(tokenHash: RefreshTokenHash, now: Instant): IO[Option[RefreshTokenRecord]] =
      records.get.map(_.find(record => record.tokenHash == tokenHash && record.isActive(now)))

    override def revoke(id: RefreshTokenId, revokedAt: Instant): IO[Boolean] =
      records.modify: current =>
        if current.exists(record => record.id == id && record.revokedAt.isEmpty) then
          (revokeWhere(current, _.id == id, revokedAt), true)
        else (current, false)

    override def rotate(
      oldHash: RefreshTokenHash,
      replacement: RefreshTokenRecord,
      now: Instant
    ): IO[Boolean] =
      records.modify: current =>
        current.find(record => record.tokenHash == oldHash && record.isActive(now)) match
          case Some(old) => (revokeWhere(current, _.id == old.id, now) :+ replacement, true)
          case None      => (current, false)

    private def revokeWhere(
      current: Vector[RefreshTokenRecord],
      predicate: RefreshTokenRecord => Boolean,
      at: Instant
    ): Vector[RefreshTokenRecord] =
      current.map(record => if predicate(record) then record.copy(revokedAt = Some(at)) else record)

  protected class StubRepository extends RefreshTokenRepository[IO]:
    override def create(token: RefreshTokenRecord): IO[Unit] =
      IO.raiseError(new AssertionError("Unexpected create"))
    override def findActiveByHash(tokenHash: RefreshTokenHash, now: Instant): IO[Option[RefreshTokenRecord]] =
      IO.raiseError(new AssertionError("Unexpected findActiveByHash"))
    override def revoke(id: RefreshTokenId, revokedAt: Instant): IO[Boolean] =
      IO.raiseError(new AssertionError("Unexpected revoke"))
    override def rotate(oldHash: RefreshTokenHash, replacement: RefreshTokenRecord, now: Instant): IO[Boolean] =
      IO.raiseError(new AssertionError("Unexpected rotate"))

  final protected case class ServiceFixture(
    service: RefreshTokenService[IO],
    repository: InMemoryRepository,
    clock: ManualClock
  )

  private def makeService(
    repository: RefreshTokenRepository[IO],
    clock: ManualClock
  ): IO[RefreshTokenService[IO]] =
    Ref.of[IO, Int](0).map: counter =>
      given Clock[IO] = clock
      LiveRefreshTokenService[IO](repository, SequentialGenerator(counter), hasher, ttl)

  protected def withService[A](test: ServiceFixture => IO[A]): IO[A] =
    for
      records    <- Ref.of[IO, Vector[RefreshTokenRecord]](Vector.empty)
      repository  = InMemoryRepository(records)
      clock      <- ManualClock.at(referenceTime)
      service    <- makeService(repository, clock)
      result     <- test(ServiceFixture(service, repository, clock))
    yield result

  protected def withStubService[A](repository: RefreshTokenRepository[IO])(
    test: RefreshTokenService[IO] => IO[A]
  ): IO[A] =
    for
      clock   <- ManualClock.at(referenceTime)
      service <- makeService(repository, clock)
      result  <- test(service)
    yield result
