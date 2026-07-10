package cortex.domain.learning

import scala.concurrent.duration.Duration

enum ContentKind:
  case Article, Video, Book

enum ContentProgress:
  case VideoAt(elapsed: Duration)
  case ArticleAt(percentage: Percentage)
  case BookAt(page: Int)

  def matches(kind: ContentKind): Boolean =
    (kind, this) match
      case (ContentKind.Book, BookAt(_))       => true
      case (ContentKind.Video, VideoAt(_))     => true
      case (ContentKind.Article, ArticleAt(_)) => true
      case _                                   => false

enum LearningEvent(val id: ContentId):
  case ContentQueued(override val id: ContentId, kind: ContentKind)           extends LearningEvent(id)
  case ContentStarted(override val id: ContentId)                             extends LearningEvent(id)
  case ContentCompleted(override val id: ContentId)                           extends LearningEvent(id)
  case ContentAbandoned(override val id: ContentId)                           extends LearningEvent(id)
  case ContentResumed(override val id: ContentId)                             extends LearningEvent(id)
  case ProgressUpdated(override val id: ContentId, position: ContentProgress) extends LearningEvent(id)
  case NoteAdded(override val id: ContentId, note: Note)                      extends LearningEvent(id)
  case NoteRemoved(override val id: ContentId, noteId: NoteId)                extends LearningEvent(id)
