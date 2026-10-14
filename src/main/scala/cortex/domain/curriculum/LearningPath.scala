package cortex.domain.curriculum

import cats.data.ValidatedNec
import cats.syntax.all.*
import cortex.domain.catalog.ResourceId
import cortex.domain.users.UserId

final case class PathItem(
  id: PathItemId,
  resourceId: ResourceId
)

final case class PathSection(
  id: SectionId,
  title: String,
  items: Vector[PathItem]
)

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
      case -1    => Left(LearningPathError.SectionNotFound(sectionId))
      case index =>
        val section = sections(index)
        rebuild(sections.updated(index, section.copy(items = section.items :+ item)))

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
    ).mapN: (normalizedTitle, normalizedSections) =>
      new LearningPath(id, authorId, normalizedTitle, description, normalizedSections)

  private def validateTitle(title: String): ValidatedNec[LearningPathError, String] =
    val normalized = title.trim
    if normalized.nonEmpty then normalized.validNec
    else LearningPathError.EmptyTitle.invalidNec

  private def validateSections(
    sections: Vector[PathSection]
  ): ValidatedNec[LearningPathError, Vector[PathSection]] =
    val normalizedSections = sections.map(section => section.copy(title = section.title.trim))

    (
      validateSectionTitles(normalizedSections),
      validateUnique(
        normalizedSections.map(_.id),
        LearningPathError.DuplicateSections(_)
      ),
      validateUnique(
        normalizedSections.flatMap(_.items.map(_.id)),
        LearningPathError.DuplicateItems(_)
      ),
      validateUnique(
        normalizedSections.flatMap(_.items.map(_.resourceId)),
        LearningPathError.DuplicateResources(_)
      )
    ).mapN((_, _, _, _) => normalizedSections)

  private def validateSectionTitles(
    sections: Vector[PathSection]
  ): ValidatedNec[LearningPathError, Unit] =
    val empty = sections.filter(_.title.isEmpty).map(_.id)
    if empty.isEmpty then ().validNec
    else LearningPathError.EmptySectionTitle(empty.toSet).invalidNec

  private def validateUnique[A](
    values: Vector[A],
    error: Set[A] => LearningPathError
  ): ValidatedNec[LearningPathError, Unit] =
    val duplicates = values.groupBy(identity).collect { case (value, xs) if xs.size > 1 => value }.toSet
    if duplicates.isEmpty then ().validNec
    else error(duplicates).invalidNec
