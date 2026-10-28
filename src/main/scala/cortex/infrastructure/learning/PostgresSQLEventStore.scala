package cortex.infrastructure.learning

import cats.effect.kernel.Sync
import cortex.domain.learning.{AppendError, ContentId, EventHistory, EventStore, EventVersion, LearningEvent}

final private[infrastructure] class PostgresSQLEventStore[F[_]: Sync] extends EventStore[F]:

  override def append(
    event: LearningEvent,
    expectedVersion: EventVersion
  ): F[Either[AppendError, EventVersion]] = ???

  override def loadById(id: ContentId): F[EventHistory] = ???
