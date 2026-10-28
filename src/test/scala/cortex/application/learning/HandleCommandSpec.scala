package cortex.application.learning

import cats.effect.{Deferred, IO, Ref}
import cats.effect.testing.scalatest.AsyncIOSpec
import cats.syntax.all.*
import cortex.domain.learning.*
import cortex.infrastructure.learning.InMemoryEventStore
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.concurrent.duration.*

class HandleCommandSpec extends AsyncFlatSpec, AsyncIOSpec, Matchers:
  private val contentId = ContentId.fromStringUnsafe("id-1")
  private val versionOne = EventVersion.fromLong(1L).fold(error => fail(s"Invalid fixture: $error"), identity)

  private def rejectingStore(error: AppendError): EventStore[IO] =
    new EventStore[IO]:
      override def loadById(id: ContentId): IO[EventHistory] =
        IO.pure(EventHistory(id, Vector(LearningEvent.ContentQueued(id, ContentKind.Book)), versionOne))

      override def append(event: LearningEvent, expectedVersion: EventVersion): IO[Either[AppendError, EventVersion]] =
        IO {
          event shouldBe LearningEvent.ContentStarted(contentId)
          expectedVersion shouldBe versionOne
          Left(error)
        }

  it should "handle enqueue command" in:
    for
      store  <- InMemoryEventStore.make[IO]
      result <- handleCommand(store)(contentId, Command.Enqueue(ContentKind.Book))
      history <- store.loadById(contentId)
    yield
      result shouldBe Right(())
      history.events shouldBe Vector(LearningEvent.ContentQueued(contentId, ContentKind.Book))
      history.version.value shouldBe 1L

  it should "append enqueue event only to the stream of the given id" in:
    val otherContentId = ContentId.fromStringUnsafe("id-2")
    for
      store       <- InMemoryEventStore.make[IO]
      _           <- handleCommand(store)(otherContentId, Command.Enqueue(ContentKind.Video))
      eventsFirst <- store.loadById(contentId)
      eventsOther <- store.loadById(otherContentId)
    yield
      eventsFirst.events shouldBe Vector.empty
      eventsOther.events shouldBe Vector(LearningEvent.ContentQueued(otherContentId, ContentKind.Video))

  it should "handle start command" in:
    for
      store  <- InMemoryEventStore.make[IO]
      _      <- handleCommand(store)(contentId, Command.Enqueue(ContentKind.Book))
      _      <- handleCommand(store)(contentId, Command.Start)
      history <- store.loadById(contentId)
    yield history.events shouldBe Vector(
      LearningEvent.ContentQueued(contentId, ContentKind.Book),
      LearningEvent.ContentStarted(contentId)
    )

  it should "return Right for successful command" in:
    for
      store  <- InMemoryEventStore.make[IO]
      result <- handleCommand(store)(contentId, Command.Enqueue(ContentKind.Book))
    yield result shouldBe Right(())

  it should "not append event and return Left when command is invalid" in:
    for
      store  <- InMemoryEventStore.make[IO]
      _      <- handleCommand(store)(contentId, Command.Enqueue(ContentKind.Book))
      _      <- handleCommand(store)(contentId, Command.Start)
      result <- handleCommand(store)(contentId, Command.Start)
      history <- store.loadById(contentId)
    yield (result, history.events) shouldBe (
      Left(
        CommandError.Validation(
          DecideError.InvalidTransition(Command.Start, ContentKind.Book, ContentStatus.InProgress)
        )
      ),
      Vector(LearningEvent.ContentQueued(contentId, ContentKind.Book), LearningEvent.ContentStarted(contentId))
    )

  it should "return Left(AlreadyExists) when enqueueing existing content" in:
    for
      store  <- InMemoryEventStore.make[IO]
      _      <- handleCommand(store)(contentId, Command.Enqueue(ContentKind.Book))
      result <- handleCommand(store)(contentId, Command.Enqueue(ContentKind.Book))
      history <- store.loadById(contentId)
    yield (result, history.events) shouldBe (
      Left(CommandError.Validation(DecideError.AlreadyExists(contentId))),
      Vector(LearningEvent.ContentQueued(contentId, ContentKind.Book))
    )

  it should "return Left(NonExistentState) for command on missing content" in:
    for
      store  <- InMemoryEventStore.make[IO]
      result <- handleCommand(store)(contentId, Command.Start)
    yield result shouldBe Left(CommandError.Validation(DecideError.NonExistentState))

  it should "propagate a version conflict instead of reporting success" in:
    val actual = EventVersion.fromLong(2L).fold(error => fail(s"Invalid fixture: $error"), identity)
    val store = rejectingStore(AppendError.VersionConflict(versionOne, actual))
    handleCommand(store)(contentId, Command.Start).map: result =>
      result shouldBe Left(CommandError.Conflict(versionOne, actual))

  it should "propagate version overflow instead of reporting success" in:
    handleCommand(rejectingStore(AppendError.VersionOverflow))(contentId, Command.Start).map: result =>
      result shouldBe Left(CommandError.VersionOverflow)

  it should "reject one of two enqueue commands that read the same version" in:
    val check = for
      underlying <- InMemoryEventStore.make[IO]
      reads      <- Ref.of[IO, Int](0)
      bothLoaded <- Deferred[IO, Unit]
      store       = new EventStore[IO]:
                      override def loadById(id: ContentId): IO[EventHistory] =
                        for
                          history <- underlying.loadById(id)
                          count   <- reads.updateAndGet(_ + 1)
                          _       <- if count == 2 then bothLoaded.complete(()).void else bothLoaded.get
                        yield history

                      override def append(
                        event: LearningEvent,
                        expectedVersion: EventVersion
                      ): IO[Either[AppendError, EventVersion]] =
                        underlying.append(event, expectedVersion)
      results <- (
                   handleCommand(store)(contentId, Command.Enqueue(ContentKind.Book)),
                   handleCommand(store)(contentId, Command.Enqueue(ContentKind.Book))
                 ).parTupled
      history <- underlying.loadById(contentId)
    yield
      List(results._1, results._2) should contain theSameElementsAs List(
        Right(()),
        Left(CommandError.Conflict(EventVersion.zero, versionOne))
      )
      history shouldBe EventHistory(
        contentId,
        Vector(LearningEvent.ContentQueued(contentId, ContentKind.Book)),
        versionOne
      )

    check.timeout(5.seconds)
