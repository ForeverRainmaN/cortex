package cortex.domain

import cortex.domain.Command.*
import cortex.domain.ContentKind.Book
import cortex.domain.ContentProgress.{BookAt, VideoAt}
import cortex.domain.ContentStatus.Completed
import cortex.domain.DecideError.{InvalidTransition, ProgressKindMismatch}
import cortex.domain.LearningEvent.*
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.concurrent.duration.DurationInt

class DecideSpec extends AnyFlatSpec with Matchers with DomainSpec:
  it should "start content from Todo state" in:
    val initialState = createInitialState(ContentStatus.Todo)
    decide(initialState, Start) shouldBe Right(LearningEvent.ContentStarted(contentId))

  it should "fail to start content when it's state is InProgress" in:
    val initialState = createInitialState(ContentStatus.InProgress)
    decide(initialState, Start) shouldBe Left(
      DecideError.InvalidTransition(Start, ContentKind.Book, ContentStatus.InProgress)
    )

  it should "abandon content from any state except Completed" in:
    decideAll(initStateInProgress, initStateTodo, initStateAbandoned)(Abandon)(Right(ContentAbandoned(contentId)))

    decide(initStateCompleted, Abandon) shouldBe Left(
      DecideError.InvalidTransition(Abandon, ContentKind.Book, Completed)
    )

  it should "complete content from any state except Completed" in:
    decideAll(
      initStateInProgress,
      initStateTodo,
      initStateAbandoned
    )(Complete)(Right(ContentCompleted(contentId)))

    decide(initStateCompleted, Complete) shouldBe Left(
      DecideError.InvalidTransition(Complete, ContentKind.Book, ContentStatus.Completed)
    )

  it should "resume content from Abandoned state, fail with others" in:
    decide(initStateAbandoned, Resume) shouldBe Right(ContentResumed(contentId))

    decide(initStateInProgress, Resume) shouldBe Left(
      DecideError.InvalidTransition(Resume, ContentKind.Book, ContentStatus.InProgress)
    )

    decide(initStateTodo, Resume) shouldBe Left(
      DecideError.InvalidTransition(Resume, ContentKind.Book, ContentStatus.Todo)
    )

    decide(initStateCompleted, Resume) shouldBe Left(
      DecideError.InvalidTransition(Resume, ContentKind.Book, ContentStatus.Completed)
    )

  it should "decide to add note" in:
    decide(initStateInProgress, AddNote(note1)) shouldBe Right(NoteAdded(contentId, note1))

  it should "decide to remove note" in:
    val stateWithNote = evolve(initStateInProgress, NoteAdded(contentId, note1))
    decide(stateWithNote, RemoveNote(note1.id)) shouldBe Right(NoteRemoved(contentId, note1.id))

  it should "update progress while in InProgress state using correct kind" in:
    decide(initStateInProgress, UpdateProgress(BookAt(55))) shouldBe Right(
      ProgressUpdated(contentId, BookAt(55))
    )

  it should "fail to update progress with VideoAt for Book" in:
    decide(initStateInProgress, UpdateProgress(VideoAt(25.seconds))) shouldBe Left(
      ProgressKindMismatch(ContentKind.Book, VideoAt(25.seconds))
    )

  it should "fail to update progress when in Todo state" in:
    decide(initStateTodo, UpdateProgress(BookAt(20))) shouldBe Left(
      InvalidTransition(UpdateProgress(BookAt(20)), ContentKind.Book, ContentStatus.Todo)
    )

  private def decideAll(states: Option[ContentState]*)(command: Command)(expected: DecideErrorOrEvent): Unit =
    states.foreach: state =>
      decide(state, command) shouldBe expected
