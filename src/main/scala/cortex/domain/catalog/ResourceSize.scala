package cortex.domain.catalog

import scala.concurrent.duration.*

enum ResourceSize:
  case Pages(value: PageCount)
  case Duration(value: PositiveDuration)

object ResourceSize:
  def pages(value: Int): Either[ResourceSizeError, ResourceSize] =
    PageCount.fromInt(value).map(ResourceSize.Pages(_))

  def duration(value: FiniteDuration): Either[ResourceSizeError, ResourceSize] =
    PositiveDuration.from(value).map(ResourceSize.Duration(_))

enum ResourceSizeError:
  case NonPositivePages(value: Int)
  case NonPositiveDuration(value: FiniteDuration)

opaque type PageCount = Int

object PageCount:
  def fromInt(value: Int): Either[ResourceSizeError, PageCount] =
    Either.cond(value > 0, value, ResourceSizeError.NonPositivePages(value))

  extension (pageCount: PageCount)
    def value: Int = pageCount

opaque type PositiveDuration = FiniteDuration

object PositiveDuration:
  def from(value: FiniteDuration): Either[ResourceSizeError, PositiveDuration] =
    Either.cond(
      value > Duration.Zero,
      value,
      ResourceSizeError.NonPositiveDuration(value)
    )

  extension (duration: PositiveDuration)
    def value: FiniteDuration = duration
