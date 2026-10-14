package cortex.domain.curriculum

import cats.data.ValidatedNec
import cats.syntax.all.*
import cortex.domain.catalog.ResourceId
import cortex.domain.users.UserId

final case class PlanItem(
  id: PlanItemId,
  resourceId: ResourceId
)

final case class LearningPlan private (
  id: LearningPlanId,
  ownerId: UserId,
  title: String,
  sourcePathId: Option[LearningPathId],
  items: Vector[PlanItem]
)

object LearningPlan:
  def make(
    id: LearningPlanId,
    ownerId: UserId,
    title: String,
    sourcePathId: Option[LearningPathId],
    items: Vector[PlanItem]
  ): ValidatedNec[LearningPlanError, LearningPlan] =
    (
      CurriculumValidation.nonEmpty(title, LearningPlanError.EmptyTitle),
      CurriculumValidation.unique(items.map(_.id), LearningPlanError.DuplicateItems(_)),
      CurriculumValidation.unique(items.map(_.resourceId), LearningPlanError.DuplicateResources(_))
    ).mapN { (normalizedTitle, _, _) =>
      new LearningPlan(
        id,
        ownerId,
        normalizedTitle,
        sourcePathId,
        items
      )
    }

  private[curriculum] def unsafeMake(
    id: LearningPlanId,
    ownerId: UserId,
    title: String,
    sourcePathId: Option[LearningPathId],
    items: Vector[PlanItem]
  ): LearningPlan = new LearningPlan(id, ownerId, title, sourcePathId, items)
