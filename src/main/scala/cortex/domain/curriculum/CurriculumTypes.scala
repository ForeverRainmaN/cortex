package cortex.domain.curriculum

import java.util.UUID
import scala.util.Try

opaque type PlanItemId = UUID

object PlanItemId:
  private def apply(value: UUID): PlanItemId = value
  
  def fromUUID(value: UUID): PlanItemId = apply(value)

  def fromString(value: String): Either[Throwable, PlanItemId] =
    Try(UUID.fromString(value)).toEither.map(apply)

  extension (id: PlanItemId) def value: UUID = id

opaque type LearningPlanId = UUID

object LearningPlanId:
  private def apply(value: UUID): LearningPlanId = value
  
  def fromUUID(value: UUID): LearningPlanId = apply(value)

  def fromString(value: String): Either[Throwable, LearningPlanId] =
    Try(UUID.fromString(value)).toEither.map(apply)

  extension (id: LearningPlanId) def value: UUID = id

opaque type LearningPathId = UUID

object LearningPathId:
  private def apply(value: UUID): LearningPathId = value
  
  def fromUUID(value: UUID): LearningPathId = apply(value)

  def fromString(value: String): Either[Throwable, LearningPathId] =
    Try(UUID.fromString(value)).toEither.map(apply)

  extension (id: LearningPathId) def value: UUID = id

opaque type SectionId = UUID

object SectionId:
  private def apply(value: UUID): SectionId = value
  
  def fromUUID(value: UUID): SectionId = apply(value)

  def fromString(value: String): Either[Throwable, SectionId] =
    Try(UUID.fromString(value)).toEither.map(apply)

  extension (id: SectionId) def value: UUID = id

opaque type PathItemId = UUID

object PathItemId:
  private def apply(value: UUID): PathItemId = value
  
  def fromUUID(value: UUID): PathItemId = apply(value)

  def fromString(value: String): Either[Throwable, PathItemId] =
    Try(UUID.fromString(value)).toEither.map(apply)

  extension (id: PathItemId) def value: UUID = id
