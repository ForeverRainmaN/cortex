package cortex.domain.learning

enum Command:
  case Start, Complete, Abandon, Resume
  case Enqueue(id: ContentId, kind: ContentKind)
  case AddNote(text: Note)
  case RemoveNote(id: NoteId)
  case UpdateProgress(position: ContentProgress)
