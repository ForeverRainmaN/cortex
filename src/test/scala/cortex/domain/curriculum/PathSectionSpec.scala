package cortex.domain.curriculum

import cats.data.NonEmptyChain
import cats.data.Validated.{Invalid, Valid}
import cats.syntax.all.*
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class PathSectionSpec extends AnyFlatSpec, Matchers, LearningPathFixture:
  it should "create a path section with valid data" in:
    val result = PathSection.make(basicsSectionId, basicsSectionTitle, Vector(scalaBasicsItem))

    result.map(section => (section.id, section.title, section.items)) shouldBe Valid(
      (basicsSectionId, basicsSectionTitle, Vector(scalaBasicsItem))
    )

  it should "normalize the title" in:
    val result = PathSection.make(basicsSectionId, "  Basics  ", Vector(scalaBasicsItem))

    result.map(_.title) shouldBe Valid(basicsSectionTitle)

  it should "reject an empty title" in:
    val result = PathSection.make(basicsSectionId, "   ", Vector(scalaBasicsItem))

    result shouldBe Invalid(NonEmptyChain.one(PathSectionError.EmptyTitle))

  it should "reject duplicate path item ids" in:
    val duplicate = patternMatchingItem.copy(id = scalaBasicsItemId)
    val result    = PathSection.make(basicsSectionId, basicsSectionTitle, Vector(scalaBasicsItem, duplicate))

    result shouldBe Invalid(NonEmptyChain.one(PathSectionError.DuplicatePathItems(Set(scalaBasicsItemId))))

  it should "add an item" in:
    basicsSection.addItem(patternMatchingItem) shouldBe Right(
      validSection(basicsSectionId, basicsSectionTitle, Vector(scalaBasicsItem, patternMatchingItem))
    )

  it should "reject adding an item with an existing id" in:
    basicsSection.addItem(scalaBasicsItem) shouldBe Left(
      PathSectionError.DuplicatePathItems(Set(scalaBasicsItemId))
    )
