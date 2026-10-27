package cortex.domain.learning

trait EventStore[F[_]]:
  def loadById(id: ContentId): F[EventHistory]
  def append(event: LearningEvent, expectedVersion: EventVersion): F[Either[AppendError, Unit]]
