package cortex.domain.catalog

import java.util.UUID
import scala.concurrent.duration.*

trait ResourceFixture:
  protected val resourceId: ResourceId = ResourceId(
    UUID.fromString("00000000-0000-0000-0000-000000000001")
  )

  protected val bookTitle      = "  Programming in Scala  "
  protected val normalizedTitle = "Programming in Scala"
  protected val author         = "Martin Odersky"
  protected val url            = " https://example.com/scala "
  protected val pageCount      = 800
  protected val videoDuration  = 90.minutes

  protected val bookSize: ResourceSize =
    ResourceSize.pages(pageCount).toOption.get

  protected val videoSize: ResourceSize =
    ResourceSize.duration(videoDuration).toOption.get
