package cortex.infrastructure

import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import cortex.domain.LearningEvent.{ContentCompleted, ContentQueued, ContentStarted}
import cortex.domain.{ContentId, ContentKind}
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers

class InMemoryEventStoreSpec extends AsyncFlatSpec, AsyncIOSpec, Matchers:
  private val contentId  = ContentId("id-1")
  private val contentId2 = ContentId("id-2")

  it should "append new event" in:
    for
      eventStore <- InMemoryEventStore.make[IO]
      _          <- eventStore.append(ContentQueued(contentId, ContentKind.Book))
      loaded     <- eventStore.loadById(contentId)
    yield loaded shouldBe List(ContentQueued(contentId, ContentKind.Book))

  it should "append several events preserving order" in:
    for
      store <- InMemoryEventStore.make[IO]

      event1 = ContentQueued(contentId, ContentKind.Book)
      event2 = ContentStarted(contentId)
      event3 = ContentCompleted(contentId)

      _ <- store.append(event1)
      _ <- store.append(event2)
      _ <- store.append(event3)

      result <- store.loadById(contentId)
    yield result shouldBe List(
      ContentQueued(contentId, ContentKind.Book),
      ContentStarted(contentId),
      ContentCompleted(contentId)
    )

  it should "return empty list for unknown id" in:
    for
      store  <- InMemoryEventStore.make[IO]
      events <- store.loadById(contentId)
    yield events shouldBe List.empty

  it should "isolate events by id" in:
    for
      store   <- InMemoryEventStore.make[IO]
      _       <- store.append(ContentQueued(contentId, ContentKind.Book))
      _       <- store.append(ContentQueued(contentId2, ContentKind.Video))
      events1 <- store.loadById(contentId)
      events2 <- store.loadById(contentId2)
    yield
      events1 shouldBe List(ContentQueued(contentId, ContentKind.Book))
      events2 shouldBe List(ContentQueued(contentId2, ContentKind.Video))
