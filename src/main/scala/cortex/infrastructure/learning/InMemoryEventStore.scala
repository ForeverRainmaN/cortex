package cortex.infrastructure.learning

import cats.effect.Ref
import cats.effect.kernel.Sync
import cats.syntax.all.*
import cortex.domain.learning.{
  AppendError,
  ContentId,
  EventHistory,
  EventStore,
  EventVersion,
  EventVersionError,
  LearningEvent
}

final private[infrastructure] class InMemoryEventStore[F[_]: Sync] private (
  storage: Ref[F, Map[ContentId, EventHistory]]
) extends EventStore[F]:
  override def loadById(id: ContentId): F[EventHistory] =
    storage.get.map: histories =>
      histories.getOrElse(
        id,
        EventHistory(id, Vector.empty, EventVersion.zero)
      )

  override def append(
    event: LearningEvent,
    expectedVersion: EventVersion
  ): F[Either[AppendError, EventVersion]] =
    storage.modify[Either[AppendError, EventVersion]]: histories =>
      val current = histories.getOrElse(
        event.id,
        EventHistory(event.id, Vector.empty, EventVersion.zero)
      )

      appendToHistory(current, event, expectedVersion) match
        case Left(error) =>
          (histories, Left(error))

        case Right(updated) =>
          (histories.updated(event.id, updated), Right(updated.version))

  private def appendToHistory(
    current: EventHistory,
    event: LearningEvent,
    expectedVersion: EventVersion
  ): Either[AppendError, EventHistory] =
    if current.version != expectedVersion then
      Left(AppendError.VersionConflict(expectedVersion, current.version))
    else
      current.version.next match
        case Left(EventVersionError.Overflow) =>
          Left(AppendError.VersionOverflow)

        case Right(newVersion) =>
          Right(
            current.copy(
              events = current.events :+ event,
              version = newVersion
            )
          )

object InMemoryEventStore:
  def make[F[_]: Sync]: F[EventStore[F]] =
    Ref
      .of[F, Map[ContentId, EventHistory]](Map.empty)
      .map(new InMemoryEventStore[F](_))
