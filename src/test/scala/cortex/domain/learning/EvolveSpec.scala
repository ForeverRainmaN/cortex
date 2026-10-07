package cortex.domain.learning

import cortex.domain.learning.*
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class EvolveSpec extends AnyFlatSpec, Matchers, DomainSpec:

  it should "create new ContentState" in:
    val event = LearningEvent.ContentQueued(contentId, ContentKind.Book)
    evolve(emptyState, event) shouldBe Some(
      ContentState(contentId, ContentKind.Book, ContentStatus.Todo, None, Vector.empty)
    )

  it should "ignore ContentQueued when content already exists" in:
    val event = LearningEvent.ContentQueued(contentId, ContentKind.Book)
    evolve(stateInProgress, event) shouldBe stateInProgress

  it should "update ContentState to InProgress when LearningEvent == ContentStarted" in:
    val event = LearningEvent.ContentStarted(contentId)
    evolve(stateTodo, event) shouldBe Some(
      ContentState(contentId, ContentKind.Book, ContentStatus.InProgress, None, Vector.empty)
    )

  it should "update ContentState to Completed when LearningEvent == ContentCompleted" in:
    val event = LearningEvent.ContentCompleted(contentId)
    evolve(stateInProgress, event) shouldBe Some(
      ContentState(contentId, ContentKind.Book, ContentStatus.Completed, None, Vector.empty)
    )

  it should "update ContentState to Abandoned when LearningEvent == ContentAbandoned & initialState == InProgress" in:
    val event = LearningEvent.ContentAbandoned(contentId)
    evolve(stateInProgress, event) shouldBe Some(
      ContentState(contentId, ContentKind.Book, ContentStatus.Abandoned, None, Vector.empty)
    )

  it should "update ContentState to Abandoned when LearningEvent == ContentAbandoned & initialState == Todo" in:
    val event = LearningEvent.ContentAbandoned(contentId)
    evolve(stateTodo, event) shouldBe Some(
      ContentState(contentId, ContentKind.Book, ContentStatus.Abandoned, None, Vector.empty)
    )

  it should "update ContentState to Abandoned when LearningEvent == ContentAbandoned & initialState == Completed" in:
    val event = LearningEvent.ContentAbandoned(contentId)
    evolve(stateCompleted, event) shouldBe Some(
      ContentState(contentId, ContentKind.Book, ContentStatus.Abandoned, None, Vector.empty)
    )

  it should "update ContentState to InProgress when LearningEvent == ContentResumed && initialState == Abandoned" in:
    val event = LearningEvent.ContentResumed(contentId)
    evolve(stateAbandoned, event) shouldBe Some(
      ContentState(contentId, ContentKind.Book, ContentStatus.InProgress, None, Vector.empty)
    )

  it should "add new note to ContentState" in:
    val event = LearningEvent.NoteAdded(contentId, firstNote)
    evolve(stateInProgress, event) shouldBe Some(
      ContentState(contentId, ContentKind.Book, ContentStatus.InProgress, None, Vector(firstNote))
    )

  it should "remove note from ContentState" in:
    val event = LearningEvent.NoteRemoved(contentId, firstNote.id)
    evolve(stateInProgress, event) shouldBe Some(
      ContentState(contentId, ContentKind.Book, ContentStatus.InProgress, None, Vector.empty)
    )

  it should "calculate correct eventual ContentState with multiple events" in:
    val events = List(
      LearningEvent.ContentQueued(contentId, ContentKind.Book),
      LearningEvent.ContentStarted(contentId),
      LearningEvent.ContentAbandoned(contentId),
      LearningEvent.NoteAdded(contentId, firstNote),
      LearningEvent.NoteAdded(contentId, secondNote),
      LearningEvent.NoteRemoved(contentId, firstNote.id),
      LearningEvent.ContentResumed(contentId),
      LearningEvent.ContentCompleted(contentId),
    )

    foldAll(events) shouldBe Map(
      contentId -> ContentState(contentId, ContentKind.Book, ContentStatus.Completed, None, Vector(secondNote))
    )
