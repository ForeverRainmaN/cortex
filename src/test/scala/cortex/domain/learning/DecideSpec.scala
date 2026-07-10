package cortex.domain.learning

import cortex.domain.learning.*
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.concurrent.duration.DurationInt

class DecideSpec extends AnyFlatSpec, Matchers, DomainSpec:
  it should "enqueue new content" in:
    decide(initStateEmpty, Command.Enqueue(contentId, ContentKind.Book)) shouldBe Right(
      LearningEvent.ContentQueued(contentId, ContentKind.Book)
    )

  it should "fail to enqueue new content if content already exists" in:
    decide(initStateInProgress, Command.Enqueue(contentId, ContentKind.Book)) shouldBe Left(
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
    decideAll(initStateInProgress, initStateTodo, initStateAbandoned)(Command.Abandon)(
      Right(LearningEvent.ContentAbandoned(contentId))
    )

    decide(initStateCompleted, Command.Abandon) shouldBe Left(
      DecideError.InvalidTransition(Command.Abandon, ContentKind.Book, ContentStatus.Completed)
    )

  it should "complete content from any state except Completed" in:
    decideAll(
      initStateInProgress,
      initStateTodo,
      initStateAbandoned
    )(Command.Complete)(Right(LearningEvent.ContentCompleted(contentId)))

    decide(initStateCompleted, Command.Complete) shouldBe Left(
      DecideError.InvalidTransition(Command.Complete, ContentKind.Book, ContentStatus.Completed)
    )

  it should "resume content from Abandoned state, fail with others" in:
    decide(initStateAbandoned, Command.Resume) shouldBe Right(LearningEvent.ContentResumed(contentId))

    decide(initStateInProgress, Command.Resume) shouldBe Left(
      DecideError.InvalidTransition(Command.Resume, ContentKind.Book, ContentStatus.InProgress)
    )

    decide(initStateTodo, Command.Resume) shouldBe Left(
      DecideError.InvalidTransition(Command.Resume, ContentKind.Book, ContentStatus.Todo)
    )

    decide(initStateCompleted, Command.Resume) shouldBe Left(
      DecideError.InvalidTransition(Command.Resume, ContentKind.Book, ContentStatus.Completed)
    )

  it should "decide to add note" in:
    decide(initStateInProgress, Command.AddNote(note1)) shouldBe Right(LearningEvent.NoteAdded(contentId, note1))

  it should "decide to remove note" in:
    val stateWithNote = evolve(initStateInProgress, LearningEvent.NoteAdded(contentId, note1))
    decide(stateWithNote, Command.RemoveNote(note1.id)) shouldBe Right(LearningEvent.NoteRemoved(contentId, note1.id))

  it should "update progress while in InProgress state using correct kind" in:
    decide(initStateInProgress, Command.UpdateProgress(ContentProgress.BookAt(55))) shouldBe Right(
      LearningEvent.ProgressUpdated(contentId, ContentProgress.BookAt(55))
    )

  it should "fail to update progress with VideoAt for Book" in:
    decide(initStateInProgress, Command.UpdateProgress(ContentProgress.VideoAt(25.seconds))) shouldBe Left(
      DecideError.ProgressKindMismatch(ContentKind.Book, ContentProgress.VideoAt(25.seconds))
    )

  it should "fail to update progress when in Todo state" in:
    decide(initStateTodo, Command.UpdateProgress(ContentProgress.BookAt(20))) shouldBe Left(
      DecideError
        .InvalidTransition(Command.UpdateProgress(ContentProgress.BookAt(20)), ContentKind.Book, ContentStatus.Todo)
    )

  private def decideAll(states: Option[ContentState]*)(command: Command)(expected: DecideErrorOrEvent): Unit =
    states.foreach: state =>
      decide(state, command) shouldBe expected
