package cortex.infrastructure.learning

import cats.effect.kernel.Sync
import cortex.domain.learning.{ContentId, EventStore, LearningEvent}

final private[infrastructure] class PostgresSQLEventStore[F[_]: Sync] extends EventStore[F]:

  override def append(event: LearningEvent): F[Unit] = ???

  override def loadById(id: ContentId): F[List[LearningEvent]] = ???