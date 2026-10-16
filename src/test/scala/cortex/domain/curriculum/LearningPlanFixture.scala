package cortex.domain.curriculum

import cats.syntax.all.*
import org.scalatest.Assertions.fail

trait LearningPlanFixture extends LearningPathFixture:
  protected val learningPlanId: LearningPlanId = LearningPlanId(uuid(40))
  protected val firstPlanItemId: PlanItemId = PlanItemId(uuid(41))
  protected val secondPlanItemId: PlanItemId = PlanItemId(uuid(42))

  protected val firstPlanItem: PlanItem = PlanItem(firstPlanItemId, scalaBasicsResourceId)
  protected val secondPlanItem: PlanItem = PlanItem(secondPlanItemId, patternMatchingResourceId)

  protected val learningPlan: LearningPlan =
    LearningPlan
      .make(
        learningPlanId,
        authorId,
        "Scala plan",
        Some(pathId),
        Vector(firstPlanItem, secondPlanItem)
      )
      .fold(errors => fail(s"Invalid learning plan fixture: ${errors.toList}"), identity)
