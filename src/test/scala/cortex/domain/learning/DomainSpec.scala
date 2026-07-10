package cortex.domain.learning

import cortex.domain.learning.*

import java.util.UUID

trait DomainSpec:
  val contentId: ContentId = ContentId("id-1")
  val note1                = Note(
    NoteId(UUID.randomUUID()),
    "test-note"
  )
  val note2                = Note(
    NoteId(UUID.randomUUID()),
    "test-note"
  )

  val initStateInProgress: Option[ContentState] = createInitialState(ContentStatus.InProgress)
  val initStateTodo: Option[ContentState]       = createInitialState(ContentStatus.Todo)
  val initStateAbandoned: Option[ContentState]  = createInitialState(ContentStatus.Abandoned)
  val initStateCompleted: Option[ContentState]  = createInitialState(ContentStatus.Completed)
  val initStateEmpty                            = Option.empty[ContentState]

  def createInitialState(
    status: ContentStatus,
    notes: Vector[Note] = Vector.empty,
    id: ContentId = contentId,
    kind: ContentKind = ContentKind.Book
  ): Option[ContentState] =
    Some(ContentState(id, kind, status, None, notes))
