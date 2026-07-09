package cortex.domain

import cortex.domain.*
import cortex.domain.DecideError.*
import cortex.domain.LearningEvent.*

private type DecideErrorOrEvent = Either[DecideError, LearningEvent]

def decide(
  state: Option[ContentState],
  command: Command
): DecideErrorOrEvent =

  def invalidTransition(
    state: ContentState,
    command: Command
  ): DecideError =
    InvalidTransition(command, state.kind, state.status)

  def transition(
    state: ContentState,
    command: Command,
    forbidden: Set[ContentStatus]
  )(
    event: => LearningEvent
  ): DecideErrorOrEvent =
    if forbidden.contains(state.status) then Left(invalidTransition(state, command))
    else Right(event)

  def validateUpdatingProgress(
    state: ContentState,
    progress: ContentProgress
  ): DecideErrorOrEvent =
    state.status match
      case ContentStatus.InProgress =>
        Either.cond(
          progress.matches(state.kind),
          ProgressUpdated(state.id, progress),
          ProgressKindMismatch(state.kind, progress)
        )

      case _ =>
        Left(
          invalidTransition(
            state,
            Command.UpdateProgress(progress)
          )
        )

  def validateAddNote(
    state: ContentState,
    note: Note
  ): DecideErrorOrEvent =
    Either.cond(
      !state.notes.exists(_.id == note.id),
      NoteAdded(state.id, note),
      NoteAlreadyExists(note.id)
    )

  def validateRemoveNote(
    state: ContentState,
    noteId: NoteId
  ): DecideErrorOrEvent =
    Either.cond(
      state.notes.exists(_.id == noteId),
      NoteRemoved(state.id, noteId),
      NoteNotFound(noteId)
    )

  (state, command) match

    case (None, Command.Enqueue(id, kind)) =>
      Right(ContentQueued(id, kind))

    case (Some(_), Command.Enqueue(id, _)) =>
      Left(AlreadyExists(id))

    case (Some(state), Command.Start) =>
      transition(
        state,
        Command.Start,
        Set(ContentStatus.InProgress)
      ):
        ContentStarted(state.id)

    case (Some(state), Command.Complete) =>
      transition(
        state,
        Command.Complete,
        Set(ContentStatus.Completed)
      ):
        ContentCompleted(state.id)

    case (Some(state), Command.Abandon) =>
      transition(
        state,
        Command.Abandon,
        Set(ContentStatus.Completed)
      ):
        ContentAbandoned(state.id)

    case (Some(state), Command.Resume) =>
      transition(
        state,
        Command.Resume,
        Set(
          ContentStatus.Todo,
          ContentStatus.InProgress,
          ContentStatus.Completed
        )
      ):
        ContentResumed(state.id)

    case (Some(state), Command.UpdateProgress(progress)) =>
      validateUpdatingProgress(state, progress)

    case (Some(state), Command.AddNote(note)) =>
      validateAddNote(state, note)

    case (Some(state), Command.RemoveNote(noteId)) =>
      validateRemoveNote(state, noteId)

    case (None, _) =>
      Left(NonExistentState)
