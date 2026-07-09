package cortex.domain

enum Command:
  case Start, Complete, Abandon, Resume
  case AddNote(text: Note)
  case UpdateProgress(position: ContentProgress)

enum DecideError:
  case InvalidTransition(
    command: Command,
    kind: ContentKind,
    status: ContentStatus
  )

  case ProgressKindMismatch(kind: ContentKind, badProgress: ContentProgress)

  case NonExistentState

  def message: String = this match
    case InvalidTransition(command, kind, status) =>
      s"$kind is in $status, can't execute command $command"

    case ProgressKindMismatch(kind, badProgress) =>
      s"progress $badProgress does not match content kind $kind"

    case NonExistentState => "state does not exist"
