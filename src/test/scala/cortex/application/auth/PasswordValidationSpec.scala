package cortex.application.auth

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class PasswordValidationSpec extends AnyFlatSpec, Matchers:
  it should "reject empty passwords and passwords exceeding 72 UTF-8 bytes" in:
    List("", "a" * 73, "я" * 37).foreach: password =>
      validatePassword(password) shouldBe
        Left("Password must contain between 1 and 72 UTF-8 bytes")

  it should "accept passwords at the byte-length boundaries" in:
    List("a", "a" * 72, "я" * 36).foreach: password =>
      validatePassword(password) shouldBe Right(())
