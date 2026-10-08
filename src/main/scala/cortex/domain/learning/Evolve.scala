package cortex.domain.learning

def evolve(state: Option[ContentState], event: LearningEvent): Option[ContentState] =
  event match
    case LearningEvent.ContentQueued(id, kind) =>
      state.orElse(Some(ContentState.initial(id, kind)))

    case LearningEvent.ContentStarted(_) =>
      state.map(_.copy(status = ContentStatus.InProgress))

    case LearningEvent.ContentCompleted(_) =>
      state.map(_.copy(status = ContentStatus.Completed))

    case LearningEvent.ContentAbandoned(_) =>
      state.map(_.copy(status = ContentStatus.Abandoned))

    case LearningEvent.ProgressUpdated(_, position) =>
      state.map(_.copy(progress = Some(position)))

    case LearningEvent.ContentResumed(_) =>
      state.map(_.copy(status = ContentStatus.InProgress))

    case LearningEvent.NoteAdded(_, note) =>
      state.map(s => s.copy(notes = s.notes :+ note))

    case LearningEvent.NoteRemoved(_, noteId) =>
      state.map(s => s.copy(notes = s.notes.filterNot(_.id == noteId)))

def fold(events: List[LearningEvent]): Option[ContentState] =
  events.foldLeft(Option.empty[ContentState])(evolve)

def foldAll(events: List[LearningEvent]): Map[ContentId, ContentState] =
  events
    .groupBy(_.id)
    .flatMap: (id, groupedEvents) =>
      fold(groupedEvents).map(id -> _)
