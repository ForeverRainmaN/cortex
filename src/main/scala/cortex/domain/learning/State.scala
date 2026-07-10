package cortex.domain.learning

enum ContentStatus:
  case Todo, InProgress, Completed, Abandoned

case class ContentState(
  id: ContentId,
  kind: ContentKind,
  status: ContentStatus,
  progress: Option[ContentProgress],
  notes: Vector[Note]
)

object ContentState:
  def initial(id: ContentId, kind: ContentKind): ContentState =
    ContentState(
      id,
      kind,
      ContentStatus.Todo,
      None,
      Vector.empty
    )
