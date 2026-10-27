package cortex.infrastructure.auth

import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import cortex.domain.auth.{RefreshToken, RefreshTokenHash}
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers

class LiveRefreshTokenHasherSpec extends AsyncFlatSpec, AsyncIOSpec, Matchers:
  private val hasher = LiveRefreshTokenHasher[IO]

  it should "hash a token as URL-safe Base64 SHA-256 without padding" in:
    hasher.hash(RefreshToken.fromString("abc")).map:
      _ shouldBe RefreshTokenHash.fromHash("ungWv48Bz-pBQUDeXa4iI7ADYaOWF3qctBD_YfIAFa0")

  it should "be deterministic" in:
    for
      first  <- hasher.hash(RefreshToken.fromString("token"))
      second <- hasher.hash(RefreshToken.fromString("token"))
    yield first shouldBe second

  it should "produce different hashes for different tokens" in:
    for
      first  <- hasher.hash(RefreshToken.fromString("token-1"))
      second <- hasher.hash(RefreshToken.fromString("token-2"))
    yield first should not be second
