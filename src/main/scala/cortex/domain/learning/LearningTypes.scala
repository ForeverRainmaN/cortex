package cortex.domain.learning

import java.util.UUID
import scala.util.Try
import scala.concurrent.duration.*

opaque type ContentId = String

object ContentId:
  private def apply(s: String): ContentId = s
  
  def fromString(s: String): Either[String, ContentId] =
    if s.nonEmpty then Right(apply(s))
    else Left("ContentId cannot be empty")
  
  def fromStringUnsafe(s: String): ContentId = apply(s)

opaque type NoteId = UUID

object NoteId:
  private def apply(u: UUID): NoteId = u
  
  def fromUUID(u: UUID): NoteId = apply(u)
  
  def fromString(s: String): Either[Throwable, NoteId] =
    Try(UUID.fromString(s)).toEither.map(apply)

final case class Note(id: NoteId, text: String)

enum PageNumberError:
  case NonPositive(value: Int)

opaque type PageNumber = Int

object PageNumber:
  private def apply(value: Int): PageNumber = value

  def fromInt(value: Int): Either[PageNumberError, PageNumber] =
    Either.cond(value > 0, apply(value), PageNumberError.NonPositive(value))

  extension (page: PageNumber)
    def value: Int = page

enum ElapsedDurationError:
  case Negative(value: FiniteDuration)

opaque type ElapsedDuration = FiniteDuration

object ElapsedDuration:
  private def apply(value: FiniteDuration): ElapsedDuration = value

  def from(value: FiniteDuration): Either[ElapsedDurationError, ElapsedDuration] =
    Either.cond(
      value >= Duration.Zero,
      apply(value),
      ElapsedDurationError.Negative(value)
    )

  extension (duration: ElapsedDuration)
    def value: FiniteDuration = duration

enum PercentageError:
  case NotANumber
  case Negative(value: Double)
  case GreaterThanOne(value: Double)

opaque type Percentage = Double

object Percentage:
  private def apply(value: Double): Percentage = value

  def fromDouble(value: Double): Either[PercentageError, Percentage] =
    if value.isNaN then Left(PercentageError.NotANumber)
    else if value < 0.0 then Left(PercentageError.Negative(value))
    else if value > 1.0 then Left(PercentageError.GreaterThanOne(value))
    else Right(apply(value))

  extension (p: Percentage)
    def toDouble: Double = p
    def format: String   = f"${p * 100}%.2f%%"
