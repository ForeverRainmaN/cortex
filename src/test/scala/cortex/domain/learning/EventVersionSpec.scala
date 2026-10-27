package cortex.domain.learning

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class EventVersionSpec extends AnyFlatSpec, Matchers:
  it should "start at zero" in:
    EventVersion.zero.value shouldBe 0L

  it should "create a version from a non-negative long" in:
    EventVersion.fromLong(0L) shouldBe Right(EventVersion.zero)
    EventVersion.fromLong(42L).map(_.value) shouldBe Right(42L)

  it should "reject negative versions" in:
    EventVersion.fromLong(-1L) shouldBe Left(EventVersionError.NegativeVersion(-1L))
    EventVersion.fromLong(Long.MinValue) shouldBe Left(EventVersionError.NegativeVersion(Long.MinValue))

  it should "accept the maximum long value" in:
    EventVersion.fromLong(Long.MaxValue).map(_.value) shouldBe Right(Long.MaxValue)

  it should "increment a version" in:
    EventVersion.zero.next.map(_.value) shouldBe Right(1L)

  it should "increment the version before the maximum long value" in:
    EventVersion
      .fromLong(Long.MaxValue - 1L)
      .flatMap(_.next)
      .map(_.value) shouldBe Right(Long.MaxValue)

  it should "reject incrementing the maximum long value" in:
    EventVersion
      .fromLong(Long.MaxValue)
      .flatMap(_.next) shouldBe Left(EventVersionError.Overflow)
