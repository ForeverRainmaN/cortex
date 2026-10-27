package cortex.domain.learning

final case class EventHistory(
  id: ContentId,
  events: Vector[LearningEvent],
  version: EventVersion
)

opaque type EventVersion = Long

object EventVersion:
  private def apply(value: Long): EventVersion = value

  val zero: EventVersion = 0L

  def fromLong(version: Long): Either[EventVersionError, EventVersion] =
    Either.cond(
      version >= 0,
      apply(version),
      EventVersionError.NegativeVersion(version)
    )

  extension (version: EventVersion)
    def value: Long = version

    def next: Either[EventVersionError, EventVersion] =
      if version == Long.MaxValue then Left(EventVersionError.Overflow)
      else Right(version + 1)

enum EventVersionError:
  case NegativeVersion(value: Long)
  case Overflow
