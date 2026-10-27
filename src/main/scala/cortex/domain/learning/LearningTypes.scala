package cortex.domain.learning

import java.util.UUID
import scala.util.Try

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

opaque type Percentage = Double

object Percentage:
  private def apply(value: Double): Percentage = value
  
  def fromDouble(value: Double): Either[String, Percentage] =
    if value < 0.0 then Left("Percentage cannot be negative")
    else if value > 1.0 then Left("Percentage cannot be greater than 1.0")
    else Right(apply(value))
  
  def unsafeFromDouble(value: Double): Percentage =
    if value < 0.0 then apply(0.0) 
    else if value > 1.0 then apply(1.0) 
    else apply(value)
  
  extension (p: Percentage)
    def toDouble: Double = p
    def format: String   = f"${p * 100}%.2f%%"
