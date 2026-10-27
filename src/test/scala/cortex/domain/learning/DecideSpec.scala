package cortex.domain.learning

import cortex.domain.learning.*
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.concurrent.duration.DurationInt

class DecideSpec extends AnyFlatSpec, Matchers, LearningFixture:
  private val page55       = PageNumber.fromInt(55).toOption.get
  private val page20       = PageNumber.fromInt(20).toOption.get
  private val elapsed25Sec = ElapsedDuration.from(25.seconds).toOption.get

  it should "enqueue new content" in:
    decide(contentId, emptyState, Command.Enqueue(ContentKind.Book)) shouldBe Right(
      LearningEvent.ContentQueued(contentId, ContentKind.Book)
    )

  it should "fail to enqueue new content if content already exists" in:
    decide(contentId, stateInProgress, Command.Enqueue(ContentKind.Book)) shouldBe Left(
      DecideError.AlreadyExists(contentId)
    )

  it should "start content from Todo state" in:
    val initialState = createInitialState(ContentStatus.Todo)
    decide(contentId, initialState, Command.Start) shouldBe Right(LearningEvent.ContentStarted(contentId))

  it should "fail to start content when it's state is InProgress" in:
    val initialState = createInitialState(ContentStatus.InProgress)
    decide(contentId, initialState, Command.Start) shouldBe Left(
      DecideError.InvalidTransition(Command.Start, ContentKind.Book, ContentStatus.InProgress)
    )

  it should "abandon content from any state except Completed" in:
    decideAll(stateInProgress, stateTodo, stateAbandoned)(Command.Abandon)(
      Right(LearningEvent.ContentAbandoned(contentId))
    )

    decide(contentId, stateCompleted, Command.Abandon) shouldBe Left(
      DecideError.InvalidTransition(Command.Abandon, ContentKind.Book, ContentStatus.Completed)
    )

  it should "complete content from any state except Completed" in:
    decideAll(
      stateInProgress,
      stateTodo,
      stateAbandoned
    )(Command.Complete)(Right(LearningEvent.ContentCompleted(contentId)))

    decide(contentId, stateCompleted, Command.Complete) shouldBe Left(
      DecideError.InvalidTransition(Command.Complete, ContentKind.Book, ContentStatus.Completed)
    )

  it should "resume content from Abandoned state, fail with others" in:
    decide(contentId, stateAbandoned, Command.Resume) shouldBe Right(LearningEvent.ContentResumed(contentId))

    decide(contentId, stateInProgress, Command.Resume) shouldBe Left(
      DecideError.InvalidTransition(Command.Resume, ContentKind.Book, ContentStatus.InProgress)
    )

    decide(contentId, stateTodo, Command.Resume) shouldBe Left(
      DecideError.InvalidTransition(Command.Resume, ContentKind.Book, ContentStatus.Todo)
    )

    decide(contentId, stateCompleted, Command.Resume) shouldBe Left(
      DecideError.InvalidTransition(Command.Resume, ContentKind.Book, ContentStatus.Completed)
    )

  it should "decide to add note" in:
    decide(contentId, stateInProgress, Command.AddNote(firstNote)) shouldBe Right(
      LearningEvent.NoteAdded(contentId, firstNote)
    )

  it should "decide to remove note" in:
    val stateWithNote = evolve(stateInProgress, LearningEvent.NoteAdded(contentId, firstNote))
    decide(contentId, stateWithNote, Command.RemoveNote(firstNote.id)) shouldBe Right(
      LearningEvent.NoteRemoved(contentId, firstNote.id)
    )

  it should "update progress while in InProgress state using correct kind" in:
    decide(contentId, stateInProgress, Command.UpdateProgress(ContentProgress.BookAt(page55))) shouldBe Right(
      LearningEvent.ProgressUpdated(contentId, ContentProgress.BookAt(page55))
    )

  it should "fail to update progress with VideoAt for Book" in:
    decide(contentId, stateInProgress, Command.UpdateProgress(ContentProgress.VideoAt(elapsed25Sec))) shouldBe Left(
      DecideError.ProgressKindMismatch(ContentKind.Book, ContentProgress.VideoAt(elapsed25Sec))
    )

  it should "fail to update progress when in Todo state" in:
    decide(contentId, stateTodo, Command.UpdateProgress(ContentProgress.BookAt(page20))) shouldBe Left(
      DecideError
        .InvalidTransition(Command.UpdateProgress(ContentProgress.BookAt(page20)), ContentKind.Book, ContentStatus.Todo)
    )

  private def decideAll(states: Option[ContentState]*)(command: Command)(expected: DecideErrorOrEvent): Unit =
    states.foreach: state =>
      decide(contentId, state, command) shouldBe expected
