package cortex.domain.learning

import cortex.domain.learning.*
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.concurrent.duration.DurationInt

class DecideSpec extends AnyFlatSpec, Matchers, DomainSpec:
  it should "enqueue new content" in:
    decide(emptyState, Command.Enqueue(contentId, ContentKind.Book)) shouldBe Right(
      LearningEvent.ContentQueued(contentId, ContentKind.Book)
    )

  it should "fail to enqueue new content if content already exists" in:
    decide(stateInProgress, Command.Enqueue(contentId, ContentKind.Book)) shouldBe Left(
      DecideError.AlreadyExists(contentId)
    )

  it should "start content from Todo state" in:
    val initialState = createInitialState(ContentStatus.Todo)
    decide(initialState, Command.Start) shouldBe Right(LearningEvent.ContentStarted(contentId))

  it should "fail to start content when it's state is InProgress" in:
    val initialState = createInitialState(ContentStatus.InProgress)
    decide(initialState, Command.Start) shouldBe Left(
      DecideError.InvalidTransition(Command.Start, ContentKind.Book, ContentStatus.InProgress)
    )

  it should "abandon content from any state except Completed" in:
    decideAll(stateInProgress, stateTodo, stateAbandoned)(Command.Abandon)(
      Right(LearningEvent.ContentAbandoned(contentId))
    )

    decide(stateCompleted, Command.Abandon) shouldBe Left(
      DecideError.InvalidTransition(Command.Abandon, ContentKind.Book, ContentStatus.Completed)
    )

  it should "complete content from any state except Completed" in:
    decideAll(
      stateInProgress,
      stateTodo,
      stateAbandoned
    )(Command.Complete)(Right(LearningEvent.ContentCompleted(contentId)))

    decide(stateCompleted, Command.Complete) shouldBe Left(
      DecideError.InvalidTransition(Command.Complete, ContentKind.Book, ContentStatus.Completed)
    )

  it should "resume content from Abandoned state, fail with others" in:
    decide(stateAbandoned, Command.Resume) shouldBe Right(LearningEvent.ContentResumed(contentId))

    decide(stateInProgress, Command.Resume) shouldBe Left(
      DecideError.InvalidTransition(Command.Resume, ContentKind.Book, ContentStatus.InProgress)
    )

    decide(stateTodo, Command.Resume) shouldBe Left(
      DecideError.InvalidTransition(Command.Resume, ContentKind.Book, ContentStatus.Todo)
    )

    decide(stateCompleted, Command.Resume) shouldBe Left(
      DecideError.InvalidTransition(Command.Resume, ContentKind.Book, ContentStatus.Completed)
    )

  it should "decide to add note" in:
    decide(stateInProgress, Command.AddNote(firstNote)) shouldBe Right(LearningEvent.NoteAdded(contentId, firstNote))

  it should "decide to remove note" in:
    val stateWithNote = evolve(stateInProgress, LearningEvent.NoteAdded(contentId, firstNote))
    decide(stateWithNote, Command.RemoveNote(firstNote.id)) shouldBe Right(
      LearningEvent.NoteRemoved(contentId, firstNote.id)
    )

  it should "update progress while in InProgress state using correct kind" in:
    decide(stateInProgress, Command.UpdateProgress(ContentProgress.BookAt(55))) shouldBe Right(
      LearningEvent.ProgressUpdated(contentId, ContentProgress.BookAt(55))
    )

  it should "fail to update progress with VideoAt for Book" in:
    decide(stateInProgress, Command.UpdateProgress(ContentProgress.VideoAt(25.seconds))) shouldBe Left(
      DecideError.ProgressKindMismatch(ContentKind.Book, ContentProgress.VideoAt(25.seconds))
    )

  it should "fail to update progress when in Todo state" in:
    decide(stateTodo, Command.UpdateProgress(ContentProgress.BookAt(20))) shouldBe Left(
      DecideError
        .InvalidTransition(Command.UpdateProgress(ContentProgress.BookAt(20)), ContentKind.Book, ContentStatus.Todo)
    )

  private def decideAll(states: Option[ContentState]*)(command: Command)(expected: DecideErrorOrEvent): Unit =
    states.foreach: state =>
      decide(state, command) shouldBe expected
