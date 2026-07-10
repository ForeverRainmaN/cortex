package cortex.infrastructure.learning

import cortex.domain.learning.{ContentId, LearningEvent}

trait EventStore[F[_]]:
  def append(event: LearningEvent): F[Unit]
  def loadById(id: ContentId): F[List[LearningEvent]]
