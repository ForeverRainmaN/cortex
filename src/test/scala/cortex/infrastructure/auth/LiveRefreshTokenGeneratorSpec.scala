package cortex.infrastructure.auth

import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import cats.syntax.all.*
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers

import java.util.Base64

class LiveRefreshTokenGeneratorSpec extends AsyncFlatSpec, AsyncIOSpec, Matchers:

  it should "generate a URL-safe Base64 token without padding that encodes 32 bytes" in:
    for
      generator <- LiveRefreshTokenGenerator[IO]
      token     <- generator.generate
    yield
      token.value should fullyMatch regex "[A-Za-z0-9_-]{43}"
      Base64.getUrlDecoder.decode(token.value) should have length 32

  it should "generate unique tokens" in:
    for
      generator <- LiveRefreshTokenGenerator[IO]
      tokens    <- generator.generate.replicateA(1000)
    yield tokens.distinct should have size 1000
