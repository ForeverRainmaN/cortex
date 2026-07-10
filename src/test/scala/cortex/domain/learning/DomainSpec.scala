package cortex.domain.learning

import cortex.domain.learning.*

trait DomainSpec:
  private val noteIdString1: String = "123e4567-e89b-12d3-a456-426614174000"
  private val noteIdString2: String = "223e4567-e89b-12d3-a456-426614174001"
  val contentId: ContentId          = ContentId("id-1")
  val note1                         = Note(NoteId.fromString(noteIdString1), "test-note")
  val note2                         = Note(NoteId.fromString(noteIdString2), "test-note")

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
