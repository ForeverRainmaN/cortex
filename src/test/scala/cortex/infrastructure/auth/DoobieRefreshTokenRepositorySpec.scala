package cortex.infrastructure.auth

import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import cortex.domain.auth.RefreshTokenRecord
import cortex.infrastructure.persistence.DoobieMappings.given
import cortex.infrastructure.persistence.DoobieSpec
import doobie.implicits.*
import doobie.postgres.implicits.*
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers

class DoobieRefreshTokenRepositorySpec
  extends AsyncFlatSpec, AsyncIOSpec, DoobieSpec, Matchers, DoobieRefreshTokenRepositoryFixture:


  it should "create a new refresh token" in:
    transactor.use: xa =>
      val program =
        for
          _          <- insertUser(xa)
          tokens     <- DoobieRefreshTokenRepository[IO](xa)
          _          <- tokens.create(testRecord)
          maybeToken <- sql"SELECT * FROM refresh_tokens WHERE id = ${testRecord.id}"
                          .query[RefreshTokenRecord]
                          .option
                          .transact(xa)
        yield maybeToken
      program.map: token =>
        token shouldBe Some(testRecord)

  it should "find active refresh token by hash" in:
    withRepositoryAndUser(DoobieRefreshTokenRepository[IO](_)): tokens =>
      for
        _          <- tokens.create(testRecord)
        maybeFound <- tokens.findActiveByHash(testTokenHash, testNow)
      yield maybeFound shouldBe Some(testRecord)

  it should "fail to find active refresh token by hash if token does not exist" in:
    withRepositoryAndUser(DoobieRefreshTokenRepository[IO](_)): tokens =>
      tokens
        .findActiveByHash(testTokenHash, testNow).map: notFound =>
          notFound shouldBe None
