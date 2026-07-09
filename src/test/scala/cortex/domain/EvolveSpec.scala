package cortex.domain

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class EvolveSpec extends AnyFlatSpec with Matchers:
  private val contentId    = ContentId("id-1")
  private val noteIdString = "123e4567-e89b-12d3-a456-426614174000"

  it should "creates new ContentState with Todo status when LearningEvent == ContentQueued" in:
    val initialState = Option.empty[ContentState]
    val event        = LearningEvent.ContentQueued(contentId, ContentKind.Book)

    evolve(initialState, event) shouldBe Some(
      ContentState(contentId, ContentKind.Book, ContentStatus.Todo, None, Vector.empty)
    )

  it should "ignore ContentQueued when content already exists" in:
    val initialState = createInitialState(ContentStatus.InProgress)
    val event        = LearningEvent.ContentQueued(contentId, ContentKind.Book)
    evolve(initialState, event) shouldBe initialState

  it should "update ContentState to InProgress when LearningEvent == ContentStarted" in:
    val initialState = createInitialState(ContentStatus.Todo)
    val event        = LearningEvent.ContentStarted(contentId)

    evolve(initialState, event) shouldBe Some(
      ContentState(contentId, ContentKind.Book, ContentStatus.InProgress, None, Vector.empty)
    )

  it should "update ContentState to Completed when LearningEvent == ContentCompleted" in:
    val initialState = createInitialState(ContentStatus.InProgress)
    val event        = LearningEvent.ContentCompleted(contentId)

    evolve(initialState, event) shouldBe Some(
      ContentState(contentId, ContentKind.Book, ContentStatus.Completed, None, Vector.empty)
    )

  it should "update ContentState to Abandoned when LearningEvent == ContentAbandoned & initialState == InProgress" in:
    val initialState = createInitialState(ContentStatus.InProgress)
    val event        = LearningEvent.ContentAbandoned(contentId)

    evolve(initialState, event) shouldBe Some(
      ContentState(contentId, ContentKind.Book, ContentStatus.Abandoned, None, Vector.empty)
    )

  it should "update ContentState to Abandoned when LearningEvent == ContentAbandoned & initialState == Todo" in:
    val initialState = createInitialState(ContentStatus.Todo)
    val event        = LearningEvent.ContentAbandoned(contentId)

    evolve(initialState, event) shouldBe Some(
      ContentState(contentId, ContentKind.Book, ContentStatus.Abandoned, None, Vector.empty)
    )

  it should "update ContentState to Abandoned when LearningEvent == ContentAbandoned & initialState == Completed" in:
    val initialState = createInitialState(ContentStatus.Completed)
    val event        = LearningEvent.ContentAbandoned(contentId)

    evolve(initialState, event) shouldBe Some(
      ContentState(contentId, ContentKind.Book, ContentStatus.Abandoned, None, Vector.empty)
    )

  it should "update ContentState to InProgress when LearningEvent == ContentResumed && initialState == Abandoned" in:
    val initialState = createInitialState(ContentStatus.Abandoned)
    val event        = LearningEvent.ContentResumed(contentId)

    evolve(initialState, event) shouldBe Some(
      ContentState(contentId, ContentKind.Book, ContentStatus.InProgress, None, Vector.empty)
    )

  it should "add new note to ContentState" in:
    val initialState = createInitialState(ContentStatus.InProgress)
    val noteId       = NoteId.fromString(noteIdString)
    val event        = LearningEvent.NoteAdded(contentId, Note(id = noteId, "test note"))

    evolve(initialState, event) shouldBe Some(
      ContentState(contentId, ContentKind.Book, ContentStatus.InProgress, None, Vector(Note(noteId, "test note")))
    )

  it should "remove note from ContentState" in:
    val noteId       = NoteId.fromString(noteIdString)
    val initialState = createInitialState(ContentStatus.InProgress, Vector(Note(noteId, "test note")))
    val event        = LearningEvent.NoteRemoved(contentId, noteId)

    evolve(initialState, event) shouldBe Some(
      ContentState(contentId, ContentKind.Book, ContentStatus.InProgress, None, Vector.empty)
    )

  private def createInitialState(status: ContentStatus, notes: Vector[Note] = Vector.empty): Option[ContentState] =
    Some(ContentState(contentId, ContentKind.Book, status, None, notes))
