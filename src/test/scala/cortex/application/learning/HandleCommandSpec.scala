package cortex.application.learning

import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import cortex.application.learning.{CommandError, handleCommand}
import cortex.domain.learning.*
import cortex.infrastructure.learning.InMemoryEventStore
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers

class HandleCommandSpec extends AsyncFlatSpec, AsyncIOSpec, Matchers:
  private val contentId = ContentId("id-1")

  it should "handle enqueue command" in:
    for
      store  <- InMemoryEventStore.make[IO]
      _      <- handleCommand(store)(contentId, Command.Enqueue(contentId, ContentKind.Book))
      events <- store.loadById(contentId)
    yield events shouldBe List(LearningEvent.ContentQueued(contentId, ContentKind.Book))

  it should "handle start command" in:
    for
      store  <- InMemoryEventStore.make[IO]
      _      <- handleCommand(store)(contentId, Command.Enqueue(contentId, ContentKind.Book))
      _      <- handleCommand(store)(contentId, Command.Start)
      events <- store.loadById(contentId)
    yield events shouldBe List(
      LearningEvent.ContentQueued(contentId, ContentKind.Book),
      LearningEvent.ContentStarted(contentId)
    )

  it should "return Right for successful command" in:
    for
      store  <- InMemoryEventStore.make[IO]
      result <- handleCommand(store)(contentId, Command.Enqueue(contentId, ContentKind.Book))
    yield result shouldBe Right(())

  it should "not append event and return Left when command is invalid" in:
    for
      store  <- InMemoryEventStore.make[IO]
      _      <- handleCommand(store)(contentId, Command.Enqueue(contentId, ContentKind.Book))
      _      <- handleCommand(store)(contentId, Command.Start)
      result <- handleCommand(store)(contentId, Command.Start)
      events <- store.loadById(contentId)
    yield
      result shouldBe Left(
        CommandError.Validation(
          DecideError.InvalidTransition(Command.Start, ContentKind.Book, ContentStatus.InProgress)
        )
      )
      events shouldBe List(
        LearningEvent.ContentQueued(contentId, ContentKind.Book),
        LearningEvent.ContentStarted(contentId)
      )

  it should "return Left(AlreadyExists) when enqueueing existing content" in:
    for
      store  <- InMemoryEventStore.make[IO]
      _      <- handleCommand(store)(contentId, Command.Enqueue(contentId, ContentKind.Book))
      result <- handleCommand(store)(contentId, Command.Enqueue(contentId, ContentKind.Book))
      events <- store.loadById(contentId)
    yield
      result shouldBe Left(CommandError.Validation(DecideError.AlreadyExists(contentId)))
      events shouldBe List(LearningEvent.ContentQueued(contentId, ContentKind.Book))

  it should "return Left(NonExistentState) for command on missing content" in:
    for
      store  <- InMemoryEventStore.make[IO]
      result <- handleCommand(store)(contentId, Command.Start)
    yield result shouldBe Left(CommandError.Validation(DecideError.NonExistentState))
