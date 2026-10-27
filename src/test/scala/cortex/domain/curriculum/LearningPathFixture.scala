package cortex.domain.curriculum

import cats.syntax.all.*
import cortex.domain.catalog.ResourceId
import cortex.domain.users.UserId
import org.scalatest.Assertions.fail

import java.util.UUID

trait LearningPathFixture:
  protected val pathId: LearningPathId = LearningPathId.fromUUID(uuid(1))
  protected val authorId: UserId       = UserId.fromUUID(uuid(2))

  protected val basicsSectionId: SectionId = SectionId.fromUUID(uuid(10))
  protected val catsSectionId: SectionId   = SectionId.fromUUID(uuid(11))

  protected val scalaBasicsItemId: PathItemId     = PathItemId.fromUUID(uuid(20))
  protected val patternMatchingItemId: PathItemId = PathItemId.fromUUID(uuid(21))
  protected val scalaWithCatsItemId: PathItemId   = PathItemId.fromUUID(uuid(22))

  protected val scalaBasicsResourceId: ResourceId     = ResourceId.fromUUID(uuid(30))
  protected val patternMatchingResourceId: ResourceId = ResourceId.fromUUID(uuid(31))
  protected val scalaWithCatsResourceId: ResourceId   = ResourceId.fromUUID(uuid(32))

  protected val pathTitle          = "Scala"
  protected val pathDescription    = Some("Learning path for Scala developers")
  protected val basicsSectionTitle = "Basics"
  protected val catsSectionTitle   = "Cats"

  protected val scalaBasicsItem: PathItem =
    PathItem(scalaBasicsItemId, scalaBasicsResourceId)

  protected val patternMatchingItem: PathItem =
    PathItem(patternMatchingItemId, patternMatchingResourceId)

  protected val scalaWithCatsItem: PathItem =
    PathItem(scalaWithCatsItemId, scalaWithCatsResourceId)

  protected val basicsSection: PathSection =
    validSection(basicsSectionId, basicsSectionTitle, Vector(scalaBasicsItem))

  protected val catsSection: PathSection =
    validSection(catsSectionId, catsSectionTitle, Vector(scalaWithCatsItem))

  protected val learningPath: LearningPath =
    validPath(pathId, authorId, pathTitle, pathDescription, Vector(basicsSection))

  protected def validSection(
    id: SectionId,
    title: String,
    items: Vector[PathItem]
  ): PathSection =
    PathSection
      .make(id, title, items)
      .fold(errs => fail(s"Invalid section fixture: ${errs.toList}"), identity)

  protected def validPath(
    id: LearningPathId,
    authorId: UserId,
    title: String,
    description: Option[String],
    sections: Vector[PathSection]
  ): LearningPath =
    LearningPath
      .make(id, authorId, title, description, sections)
      .fold(errs => fail(s"Invalid learning path fixture: ${errs.toList}"), identity)

  protected def unsafeSection(
    id: SectionId,
    title: String,
    items: Vector[PathItem]
  ): PathSection =
    PathSection.unsafeMake(id, title, items)

  protected def unsafePath(
    id: LearningPathId,
    authorId: UserId,
    title: String,
    description: Option[String],
    sections: Vector[PathSection]
  ): LearningPath =
    LearningPath.unsafeMake(id, authorId, title, description, sections)

  protected def uuid(index: Int): UUID =
    UUID.fromString(f"00000000-0000-0000-0000-$index%012d")
