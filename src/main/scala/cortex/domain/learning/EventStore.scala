package cortex.domain.learning

trait EventStore[F[_]]:
  def append(event: LearningEvent): F[Unit]
  def loadById(id: ContentId): F[List[LearningEvent]]
