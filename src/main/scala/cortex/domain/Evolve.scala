package cortex.domain

def evolve(state: Option[ContentState], event: LearningEvent): Option[ContentState] =
  (state, event) match
    case (None, LearningEvent.ContentQueued(id, kind))         =>
      Some(ContentState(id, kind, ContentStatus.Todo, None, Vector.empty))
    case (Some(s), LearningEvent.ContentStarted(_))            =>
      Some(s.copy(status = ContentStatus.InProgress))
    case (Some(s), LearningEvent.ContentCompleted(_))          =>
      Some(s.copy(status = ContentStatus.Completed))
    case (Some(s), LearningEvent.ContentAbandoned(_))          =>
      Some(s.copy(status = ContentStatus.Abandoned))
    case (Some(s), LearningEvent.ProgressUpdated(_, position)) =>
      Some(s.copy(progress = Some(position)))
    case (Some(s), LearningEvent.ContentResumed(_))            =>
      Some(s.copy(status = ContentStatus.InProgress))
    case (Some(s), LearningEvent.NoteAdded(_, note))           =>
      Some(s.copy(notes = s.notes :+ note))
    case (Some(s), LearningEvent.NoteRemoved(_, noteId))       =>
      Some(s.copy(notes = s.notes.filterNot(_.id == noteId)))
    case (state, _)                                            => state

def foldAll(events: List[LearningEvent]): Map[ContentId, ContentState] =
  events
    .groupBy(_.id)
    .flatMap: (id, events) =>
      events.foldLeft(Option.empty[ContentState])(evolve).map(id -> _)
