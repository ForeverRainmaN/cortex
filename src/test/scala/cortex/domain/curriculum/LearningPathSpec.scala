package cortex.domain.curriculum

import cats.data.NonEmptyChain
import cats.data.Validated.{Invalid, Valid}
import cats.syntax.all.*
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class LearningPathSpec extends AnyFlatSpec, Matchers, LearningPathFixture:
  it should "create a learning path with valid data" in:
    val result = LearningPath.make(pathId, authorId, pathTitle, pathDescription, Vector(basicsSection))

    result.map(path => (path.id, path.authorId, path.title, path.description, path.sections)) shouldBe Valid(
      (pathId, authorId, pathTitle, pathDescription, Vector(basicsSection))
    )

  it should "normalize path and section titles" in:
    val section = basicsSection.copy(title = s"  $basicsSectionTitle  ")
    val result  = LearningPath.make(pathId, authorId, s"  $pathTitle  ", pathDescription, Vector(section))

    result.map(path => (path.title, path.sections.map(_.title))) shouldBe Valid(
      (pathTitle, Vector(basicsSectionTitle))
    )

  it should "reject an empty title" in:
    val result = LearningPath.make(pathId, authorId, "   ", pathDescription, Vector(basicsSection))

    result shouldBe Invalid(NonEmptyChain.one(LearningPathError.EmptyTitle))

  it should "reject an empty section title" in:
    val section = basicsSection.copy(title = "   ")
    val result  = LearningPath.make(pathId, authorId, pathTitle, pathDescription, Vector(section))

    result shouldBe Invalid(NonEmptyChain.one(LearningPathError.EmptySectionTitle(Set(basicsSectionId))))

  it should "reject duplicate sections" in:
    val result =
      LearningPath.make(pathId, authorId, pathTitle, pathDescription, Vector(basicsSection, basicsSection))

    result.leftMap(_.toList) shouldBe Invalid(
      List(
        LearningPathError.DuplicateSections(Set(basicsSectionId)),
        LearningPathError.DuplicateItems(Set(scalaBasicsItemId)),
        LearningPathError.DuplicateResources(Set(scalaBasicsResourceId))
      )
    )

  it should "reject duplicate resources across sections" in:
    val duplicate = catsSection.copy(items = Vector(PathItem(scalaWithCatsItemId, scalaBasicsResourceId)))
    val result    =
      LearningPath.make(pathId, authorId, pathTitle, pathDescription, Vector(basicsSection, duplicate))

    result shouldBe Invalid(NonEmptyChain.one(LearningPathError.DuplicateResources(Set(scalaBasicsResourceId))))

  it should "accumulate independent errors" in:
    val section = basicsSection.copy(title = "   ")
    val result  = LearningPath.make(pathId, authorId, "   ", pathDescription, Vector(section))

    result.leftMap(_.toList) shouldBe Invalid(
      List(
        LearningPathError.EmptyTitle,
        LearningPathError.EmptySectionTitle(Set(basicsSectionId))
      )
    )

  it should "add a new section" in:
    learningPath.addSection(catsSection).map(_.sections) shouldBe Right(Vector(basicsSection, catsSection))

  it should "reject adding a section with an existing id" in:
    val section = catsSection.copy(id = basicsSectionId, items = Vector(scalaWithCatsItem))

    learningPath.addSection(section) shouldBe Left(LearningPathError.DuplicateSections(Set(basicsSectionId)))

  it should "reject adding a section with an already used resource" in:
    val section = catsSection.copy(items = Vector(PathItem(scalaWithCatsItemId, scalaBasicsResourceId)))

    learningPath.addSection(section) shouldBe Left(
      LearningPathError.DuplicateResources(Set(scalaBasicsResourceId))
    )

  it should "add an item to an existing section" in:
    learningPath.addItem(basicsSectionId, patternMatchingItem).map(_.sections.map(_.items)) shouldBe Right(
      Vector(Vector(scalaBasicsItem, patternMatchingItem))
    )

  it should "reject adding an item to an unknown section" in:
    learningPath.addItem(catsSectionId, patternMatchingItem) shouldBe Left(
      LearningPathError.SectionNotFound(catsSectionId)
    )

  it should "reject adding an item with an already used resource" in:
    val item = PathItem(patternMatchingItemId, scalaBasicsResourceId)

    learningPath.addItem(basicsSectionId, item) shouldBe Left(
      LearningPathError.DuplicateResources(Set(scalaBasicsResourceId))
    )

  it should "reject adding an item with an existing id" in:
    val item = PathItem(scalaBasicsItemId, patternMatchingResourceId)

    learningPath.addItem(basicsSectionId, item) shouldBe Left(
      LearningPathError.DuplicateItems(Set(scalaBasicsItemId))
    )
