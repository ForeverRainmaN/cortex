package cortex.domain.learning

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.concurrent.duration.*

class LearningTypesSpec extends AnyFlatSpec, Matchers:
  it should "accept a positive page number" in:
    PageNumber.fromInt(1) shouldBe Right(PageNumber.fromInt(1).toOption.get)

  it should "reject a non-positive page number" in:
    PageNumber.fromInt(0) shouldBe Left(PageNumberError.NonPositive(0))
    PageNumber.fromInt(-1) shouldBe Left(PageNumberError.NonPositive(-1))

  it should "accept zero elapsed duration" in:
    ElapsedDuration.from(Duration.Zero).isRight shouldBe true

  it should "reject negative elapsed duration" in:
    ElapsedDuration.from((-1).second) shouldBe Left(ElapsedDurationError.Negative((-1).second))

  it should "accept percentages in the closed interval from zero to one" in:
    Percentage.fromDouble(0.0).isRight shouldBe true
    Percentage.fromDouble(1.0).isRight shouldBe true

  it should "reject invalid percentages" in:
    Percentage.fromDouble(Double.NaN) shouldBe Left(PercentageError.NotANumber)
    Percentage.fromDouble(-0.1) shouldBe Left(PercentageError.Negative(-0.1))
    Percentage.fromDouble(1.1) shouldBe Left(PercentageError.GreaterThanOne(1.1))
