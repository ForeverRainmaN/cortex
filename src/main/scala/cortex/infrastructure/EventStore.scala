package cortex.infrastructure

import cortex.domain.{ContentId, LearningEvent}

trait EventStore[F[_]]:
  def append(event: LearningEvent): F[Unit]
  def loadById(id: ContentId): F[List[LearningEvent]]
