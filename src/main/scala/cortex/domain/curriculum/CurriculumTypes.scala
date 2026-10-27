package cortex.domain.curriculum

import java.util.UUID
import scala.util.Try

opaque type PlanItemId = UUID

object PlanItemId:
  def apply(value: UUID): PlanItemId = value

  def fromString(value: String): Either[Throwable, PlanItemId] =
    Try(UUID.fromString(value)).toEither

  extension (id: PlanItemId) def value: UUID = id

opaque type LearningPlanId = UUID

object LearningPlanId:
  def apply(value: UUID): LearningPlanId = value

  def fromString(value: String): Either[Throwable, LearningPlanId] =
    Try(UUID.fromString(value)).toEither

  extension (id: LearningPlanId) def value: UUID = id

opaque type LearningPathId = UUID

object LearningPathId:
  def apply(value: UUID): LearningPathId = value

  def fromString(value: String): Either[Throwable, LearningPathId] =
    Try(UUID.fromString(value)).toEither

  extension (id: LearningPathId) def value: UUID = id

opaque type SectionId = UUID

object SectionId:
  def apply(value: UUID): SectionId = value

  def fromString(value: String): Either[Throwable, SectionId] =
    Try(UUID.fromString(value)).toEither

  extension (id: SectionId) def value: UUID = id

opaque type PathItemId = UUID

object PathItemId:
  def apply(value: UUID): PathItemId = value

  def fromString(value: String): Either[Throwable, PathItemId] =
    Try(UUID.fromString(value)).toEither

  extension (id: PathItemId) def value: UUID = id
