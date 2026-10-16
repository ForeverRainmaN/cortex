package cortex.domain.curriculum

import cats.data.NonEmptyChain
import cats.data.Validated.{Invalid, Valid}
import cats.syntax.all.*
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class LearningPlanSpec extends AnyFlatSpec, Matchers, LearningPlanFixture:
  it should "create a learning plan with valid data" in:
    val result = LearningPlan.make(
      learningPlanId,
      authorId,
      "Scala plan",
      Some(pathId),
      Vector(firstPlanItem, secondPlanItem)
    )

    result.map(plan => (plan.id, plan.ownerId, plan.title, plan.sourcePathId, plan.items)) shouldBe Valid(
      (learningPlanId, authorId, "Scala plan", Some(pathId), Vector(firstPlanItem, secondPlanItem))
    )

  it should "normalize the title" in:
    val result = LearningPlan.make(
      learningPlanId,
      authorId,
      "  Scala plan  ",
      None,
      Vector(firstPlanItem)
    )

    result.map(_.title) shouldBe Valid("Scala plan")

  it should "reject an empty title" in:
    val result = LearningPlan.make(learningPlanId, authorId, "   ", None, Vector.empty)

    result shouldBe Invalid(NonEmptyChain.one(LearningPlanError.EmptyTitle))

  it should "reject duplicate plan item ids" in:
    val duplicate = secondPlanItem.copy(id = firstPlanItemId)
    val result    = LearningPlan.make(learningPlanId, authorId, "Scala plan", None, Vector(firstPlanItem, duplicate))

    result shouldBe Invalid(NonEmptyChain.one(LearningPlanError.DuplicateItems(Set(firstPlanItemId))))

  it should "reject duplicate resources" in:
    val duplicate = secondPlanItem.copy(resourceId = firstPlanItem.resourceId)
    val result    = LearningPlan.make(learningPlanId, authorId, "Scala plan", None, Vector(firstPlanItem, duplicate))

    result shouldBe Invalid(
      NonEmptyChain.one(LearningPlanError.DuplicateResources(Set(firstPlanItem.resourceId)))
    )

  it should "accumulate independent errors" in:
    val duplicate = secondPlanItem.copy(id = firstPlanItemId, resourceId = firstPlanItem.resourceId)
    val result    = LearningPlan.make(learningPlanId, authorId, "   ", None, Vector(firstPlanItem, duplicate))

    result.leftMap(_.toList) shouldBe Invalid(
      List(
        LearningPlanError.EmptyTitle,
        LearningPlanError.DuplicateItems(Set(firstPlanItemId)),
        LearningPlanError.DuplicateResources(Set(firstPlanItem.resourceId))
      )
    )
