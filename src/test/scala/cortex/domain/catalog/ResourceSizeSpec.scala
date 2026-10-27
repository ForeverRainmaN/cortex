package cortex.domain.catalog

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.concurrent.duration.Duration

class ResourceSizeSpec extends AnyFlatSpec, Matchers, ResourceFixture:
  it should "create a positive page count" in:
    ResourceSize.pages(pageCount) shouldBe Right(bookSize)

  it should "reject a non-positive page count" in:
    ResourceSize.pages(0) shouldBe Left(ResourceSizeError.NonPositivePages(0))
  ResourceSize.pages(-1) shouldBe Left(ResourceSizeError.NonPositivePages(-1))

  it should "create a positive duration" in:
    ResourceSize.duration(videoDuration) shouldBe Right(videoSize)

  it should "reject a non-positive duration" in:
    ResourceSize.duration(Duration.Zero) shouldBe Left(ResourceSizeError.NonPositiveDuration(Duration.Zero))
