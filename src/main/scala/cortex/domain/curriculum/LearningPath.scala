package cortex.domain.curriculum

import cats.data.ValidatedNec
import cats.syntax.all.*
import cortex.domain.catalog.ResourceId
import cortex.domain.curriculum.PathSectionError
import cortex.domain.curriculum.{LearningPathError, LearningPathId, PathItemId, SectionId}
import cortex.domain.users.UserId

final case class PathItem(
  id: PathItemId,
  resourceId: ResourceId
)

final case class PathSection private (
  id: SectionId,
  title: String,
  items: Vector[PathItem]
):
  def addItem(item: PathItem): Either[PathSectionError, PathSection] =
    PathSection
      .make(id, title, items :+ item)
      .toEither
      .leftMap(_.head)

object PathSection:
  def make(
    id: SectionId,
    title: String,
    items: Vector[PathItem]
  ): ValidatedNec[PathSectionError, PathSection] =
    (
      CurriculumValidation.unique(items.map(_.id), PathSectionError.DuplicatePathItems(_)),
      CurriculumValidation.nonEmpty(title, PathSectionError.EmptyTitle)
    ).mapN { (_, normalizedTitle) =>
      new PathSection(id, normalizedTitle, items)
    }

  private[curriculum] def unsafeMake(
    id: SectionId,
    title: String,
    items: Vector[PathItem]
  ): PathSection = new PathSection(id, title, items)

final case class LearningPath private (
  id: LearningPathId,
  authorId: UserId,
  title: String,
  description: Option[String],
  sections: Vector[PathSection]
):
  def addSection(section: PathSection): Either[LearningPathError, LearningPath] =
    rebuild(sections :+ section)

  def addItem(sectionId: SectionId, item: PathItem): Either[LearningPathError, LearningPath] =
    sections.indexWhere(_.id == sectionId) match
      case -1    =>
        Left(LearningPathError.SectionNotFound(sectionId))
      case index =>
        val current        = sections(index)
        val updatedSection = PathSection.unsafeMake(current.id, current.title, current.items :+ item)
        rebuild(sections.updated(index, updatedSection))

  private def rebuild(updatedSections: Vector[PathSection]): Either[LearningPathError, LearningPath] =
    LearningPath
      .make(id, authorId, title, description, updatedSections)
      .toEither
      .leftMap(_.head)

object LearningPath:
  def make(
    id: LearningPathId,
    authorId: UserId,
    title: String,
    description: Option[String],
    sections: Vector[PathSection]
  ): ValidatedNec[LearningPathError, LearningPath] =
    (
      validateTitle(title),
      validateSections(sections)
    ).mapN { (normalizedTitle, validatedSections) =>
      new LearningPath(id, authorId, normalizedTitle, description, validatedSections)
    }

  private[curriculum] def unsafeMake(
    id: LearningPathId,
    authorId: UserId,
    title: String,
    description: Option[String],
    sections: Vector[PathSection]
  ): LearningPath = new LearningPath(id, authorId, title, description, sections)

  private def validateTitle(title: String): ValidatedNec[LearningPathError, String] =
    CurriculumValidation.nonEmpty(title, LearningPathError.EmptyTitle)

  private def validateSections(
    sections: Vector[PathSection]
  ): ValidatedNec[LearningPathError, Vector[PathSection]] =
    (
      validateSectionTitles(sections),
      CurriculumValidation.unique(sections.map(_.id), LearningPathError.DuplicateSections(_)),
      CurriculumValidation.unique(
        sections.flatMap(_.items.map(_.id)),
        LearningPathError.DuplicateItems(_)
      ),
      CurriculumValidation.unique(
        sections.flatMap(_.items.map(_.resourceId)),
        LearningPathError.DuplicateResources(_)
      )
    ).mapN { (_, _, _, _) =>
      sections
    }

  private def validateSectionTitles(
    sections: Vector[PathSection]
  ): ValidatedNec[LearningPathError, Unit] =
    CurriculumValidation.nonEmptyKeys(
      sections,
      _.title,
      _.id,
      LearningPathError.EmptySectionTitle(_)
    )
