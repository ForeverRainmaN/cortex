package cortex.domain.learning

import cortex.domain.learning.{Command, ContentId, ContentKind, ContentProgress, ContentStatus, NoteId}

enum DecideError:
  case InvalidTransition(
    command: Command,
    kind: ContentKind,
    status: ContentStatus
  )

  case NoteNotFound(noteId: NoteId)

  case NoteAlreadyExists(noteId: NoteId)

  case AlreadyExists(id: ContentId)

  case ProgressKindMismatch(kind: ContentKind, badProgress: ContentProgress)

  case NonExistentState

  def message: String = this match
    case InvalidTransition(command, kind, status) =>
      s"$kind is in $status, can't execute command $command"

    case ProgressKindMismatch(kind, badProgress) =>
      s"progress $badProgress does not match content kind $kind"

    case AlreadyExists(id: ContentId) =>
      s"content with id=$id already exists"

    case NoteNotFound(noteId) => s"note $noteId not found"

    case NoteAlreadyExists(noteId) => s"note $noteId already exists"

    case NonExistentState => "state does not exist"
