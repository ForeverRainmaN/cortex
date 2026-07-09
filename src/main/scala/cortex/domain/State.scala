package cortex.domain

enum ContentStatus:
  case Todo, InProgress, Completed, Abandoned

case class ContentState(
  id: ContentId,
  kind: ContentKind,
  status: ContentStatus,
  progress: Option[ContentProgress],
  notes: Vector[Note]
)
