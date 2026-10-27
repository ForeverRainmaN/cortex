package cortex.domain.catalog

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.concurrent.duration.*

class ResourceSpec extends AnyFlatSpec, Matchers, ResourceFixture:
  it should "create a resource with normalized text fields" in:
    Resource.make(
      id = resourceId,
      title = bookTitle,
      kind = ResourceKind.Book,
      author = Some(author),
      url = Some(url),
      size = Some(bookSize)
    ).map(resource =>
      (
        resource.id,
        resource.title,
        resource.kind,
        resource.author,
        resource.url,
        resource.size
      )
    ) shouldBe Right(
      (
        resourceId,
        normalizedTitle,
        ResourceKind.Book,
        Some(author),
        Some(url.trim),
        Some(bookSize)
      )
    )

  it should "reject an empty title" in:
    Resource.make(resourceId, "  ", ResourceKind.Article) shouldBe Left(ResourceError.EmptyTitle)

  it should "reject an empty URL" in:
    Resource.make(resourceId, "Article", ResourceKind.Article, url = Some("  ")) shouldBe Left(
      ResourceError.EmptyUrl
    )

  it should "accept pages for books and articles" in:
    Resource.make(resourceId, "Book", ResourceKind.Book, size = Some(bookSize)).isRight shouldBe true
    Resource.make(resourceId, "Article", ResourceKind.Article, size = Some(bookSize)).isRight shouldBe true

  it should "accept duration for videos and courses" in:
    Resource.make(resourceId, "Video", ResourceKind.Video, size = Some(videoSize)).isRight shouldBe true
    Resource.make(resourceId, "Course", ResourceKind.Course, size = Some(videoSize)).isRight shouldBe true

  it should "reject an incompatible size" in:
    Resource.make(resourceId, "Video", ResourceKind.Video, size = Some(bookSize)) shouldBe Left(
      ResourceError.InvalidSize(ResourceKind.Video, bookSize)
    )

    Resource.make(resourceId, "Book", ResourceKind.Book, size = Some(videoSize)) shouldBe Left(
      ResourceError.InvalidSize(ResourceKind.Book, videoSize)
    )
