package cortex.domain

enum Command:
  case Start, Complete, Abandon, Resume
  case Enqueue(id: ContentId, kind: ContentKind)
  case AddNote(text: Note)
  case RemoveNote(id: NoteId)
  case UpdateProgress(position: ContentProgress)

enum DecideError:
  case InvalidTransition(
    command: Command,
    kind: ContentKind,
    status: ContentStatus
  )

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

    case NonExistentState => "state does not exist"
