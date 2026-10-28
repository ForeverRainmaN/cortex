package cortex.infrastructure.learning

import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import cats.syntax.all.*
import cortex.domain.learning.*
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers

class InMemoryEventStoreSpec extends AsyncFlatSpec, AsyncIOSpec, Matchers:
  private val contentId      = ContentId.fromStringUnsafe("id-1")
  private val otherContentId = ContentId.fromStringUnsafe("id-2")
  private val firstEvent     = LearningEvent.ContentQueued(contentId, ContentKind.Book)
  private val secondEvent    = LearningEvent.ContentStarted(contentId)

  private def version(value: Long): EventVersion =
    EventVersion.fromLong(value).toOption.get

  it should "append a new event at version zero" in:
    for
      store  <- InMemoryEventStore.make[IO]
      result <- store.append(firstEvent, EventVersion.zero)
      loaded <- store.loadById(contentId)
    yield
      result shouldBe Right(version(1L))
      loaded.events shouldBe Vector(firstEvent)
      loaded.version.value shouldBe 1L

  it should "append several events preserving order and version" in:
    for
      store <- InMemoryEventStore.make[IO]
      firstResult <- store.append(firstEvent, EventVersion.zero)
      secondResult <- store.append(secondEvent, version(1L))
      thirdResult <- store.append(LearningEvent.ContentCompleted(contentId), version(2L))
      loaded <- store.loadById(contentId)
    yield
      firstResult shouldBe Right(version(1L))
      secondResult shouldBe Right(version(2L))
      thirdResult shouldBe Right(version(3L))
      loaded.events shouldBe Vector(
        firstEvent,
        secondEvent,
        LearningEvent.ContentCompleted(contentId)
      )
      loaded.version.value shouldBe 3L

  it should "return empty history at version zero for an unknown id" in:
    for
      store  <- InMemoryEventStore.make[IO]
      loaded <- store.loadById(contentId)
    yield loaded shouldBe EventHistory(contentId, Vector.empty, EventVersion.zero)

  it should "isolate event histories by id" in:
    val otherEvent = LearningEvent.ContentQueued(otherContentId, ContentKind.Video)
    for
      store <- InMemoryEventStore.make[IO]
      _     <- store.append(firstEvent, EventVersion.zero)
      _     <- store.append(otherEvent, EventVersion.zero)
      first <- store.loadById(contentId)
      other <- store.loadById(otherContentId)
    yield
      first.events shouldBe Vector(firstEvent)
      other.events shouldBe Vector(otherEvent)

  it should "reject an append based on a stale version" in:
    for
      store  <- InMemoryEventStore.make[IO]
      first  <- store.append(firstEvent, EventVersion.zero)
      stale  <- store.append(secondEvent, EventVersion.zero)
      loaded <- store.loadById(contentId)
    yield
      first shouldBe Right(version(1L))
      stale shouldBe Left(AppendError.VersionConflict(EventVersion.zero, version(1L)))
      loaded.events shouldBe Vector(firstEvent)
      loaded.version.value shouldBe 1L

  it should "allow only one concurrent append for the same expected version" in:
    for
      store  <- InMemoryEventStore.make[IO]
      result <- (
                  store.append(firstEvent, EventVersion.zero),
                  store.append(secondEvent, EventVersion.zero)
                ).parMapN((first, second) => (first, second))
      loaded <- store.loadById(contentId)
    yield
      List(result._1, result._2).count(_.isRight) shouldBe 1
      List(result._1, result._2).count(_.isLeft) shouldBe 1
      loaded.events.size shouldBe 1
      loaded.version.value shouldBe 1L
