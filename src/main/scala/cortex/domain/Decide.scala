package cortex.domain

import cortex.domain.LearningEvent.*

private type DecideErrorOrEvent = Either[DecideError, LearningEvent]

def decide(state: Option[ContentState], command: Command): DecideErrorOrEvent =
  def validateStarting(state: ContentState): DecideErrorOrEvent =
    state.status match
      case ContentStatus.InProgress =>
        Left(DecideError.InvalidTransition(Command.Start, state.kind, state.status))
      case _                        => Right(ContentStarted(state.id))

  def validateAbandoning(state: ContentState): DecideErrorOrEvent =
    state.status match
      case ContentStatus.Completed =>
        Left(DecideError.InvalidTransition(Command.Abandon, state.kind, state.status))
      case _                       => Right(ContentAbandoned(state.id))

  def validateCompletion(state: ContentState): DecideErrorOrEvent =
    state.status match
      case ContentStatus.Completed =>
        Left(DecideError.InvalidTransition(Command.Complete, state.kind, state.status))
      case _                       => Right(ContentCompleted(state.id))

  def validateResuming(state: ContentState): DecideErrorOrEvent =
    state.status match
      case ContentStatus.Abandoned => Right(ContentResumed(state.id))
      case _                       =>
        Left(DecideError.InvalidTransition(Command.Resume, state.kind, state.status))

  def validateUpdatingProgress(s: ContentState, progress: ContentProgress): DecideErrorOrEvent =
    s.status match
      case ContentStatus.InProgress =>
        (s.kind, progress) match
          case (ContentKind.Book, ContentProgress.BookAt(_))       => Right(ProgressUpdated(s.id, progress))
          case (ContentKind.Video, ContentProgress.VideoAt(_))     => Right(ProgressUpdated(s.id, progress))
          case (ContentKind.Article, ContentProgress.ArticleAt(_)) => Right(ProgressUpdated(s.id, progress))
          case (kind, bad)                                         => Left(DecideError.ProgressKindMismatch(kind, bad))
      case other                    =>
        Left(DecideError.InvalidTransition(Command.UpdateProgress(progress), s.kind, other))

  (state, command) match
    case (Some(s), Command.Complete)                 =>
      validateCompletion(s)
    case (Some(s), Command.Start)                    =>
      validateStarting(s)
    case (Some(s), Command.Abandon)                  =>
      validateAbandoning(s)
    case (Some(s), Command.Resume)                   =>
      validateResuming(s)
    case (Some(s), Command.UpdateProgress(progress)) =>
      validateUpdatingProgress(s, progress)
    case (Some(s), Command.AddNote(text))            =>
      Right(NoteAdded(s.id, text))
    case (Some(s), Command.RemoveNote(noteId))       =>
      Right(NoteRemoved(s.id, noteId))
    case (None, _)                                   =>
      Left(DecideError.NonExistentState)
