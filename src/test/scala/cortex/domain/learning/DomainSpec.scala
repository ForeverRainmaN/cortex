package cortex.domain.learning

import cortex.domain.learning.*

import java.util.UUID

trait DomainSpec:
  protected val contentId: ContentId = ContentId("id-1")
  protected val firstNote: Note = Note(
    NoteId(UUID.fromString("00000000-0000-0000-0000-000000000001")),
    "first note"
  )
  protected val secondNote: Note = Note(
    NoteId(UUID.fromString("00000000-0000-0000-0000-000000000002")),
    "second note"
  )

  protected val stateInProgress: Option[ContentState] = createInitialState(ContentStatus.InProgress)
  protected val stateTodo: Option[ContentState]       = createInitialState(ContentStatus.Todo)
  protected val stateAbandoned: Option[ContentState]  = createInitialState(ContentStatus.Abandoned)
  protected val stateCompleted: Option[ContentState]  = createInitialState(ContentStatus.Completed)
  protected val emptyState: Option[ContentState]     = None

  protected def createInitialState(
    status: ContentStatus,
    notes: Vector[Note] = Vector.empty,
    id: ContentId = contentId,
    kind: ContentKind = ContentKind.Book
  ): Option[ContentState] =
    Some(ContentState(id, kind, status, None, notes))
