package cortex.infrastructure

import cats.effect.Ref
import cats.effect.kernel.Sync
import cats.syntax.all.*
import cortex.domain.{ContentId, LearningEvent}

private[infrastructure] class InMemoryEventStore[F[_]: Sync] private (
  val storage: Ref[F, Map[ContentId, List[LearningEvent]]]
) extends EventStore[F]:
  override def append(event: LearningEvent): F[Unit] =
    storage.update: events =>
      events.updatedWith(event.id):
        case Some(events) => Some(events :+ event)
        case None         => Some(List(event))

  override def loadById(id: ContentId): F[List[LearningEvent]] =
    storage.get.map(_.getOrElse(id, List.empty))

object InMemoryEventStore:
  def make[F[_]: Sync]: F[EventStore[F]] =
    Ref
      .of[F, Map[ContentId, List[LearningEvent]]](Map.empty)
      .map(new InMemoryEventStore[F](_))
