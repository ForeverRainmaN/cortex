package cortex.domain.learning

import java.util.UUID
import scala.util.Try

opaque type ContentId = String

object ContentId:
  def apply(s: String): ContentId = s

opaque type NoteId = UUID

object NoteId:
  def apply(u: UUID): NoteId                           = u
  def fromString(s: String): Either[Throwable, NoteId] =
    Try(UUID.fromString(s)).toEither

final case class Note(id: NoteId, text: String)

opaque type Percentage = Double

object Percentage:
  def apply(value: Double): Percentage =
    if value < 0.0 then 0.0 else if value > 1.0 then 1.0 else value
  extension (p: Percentage)
    def toDouble: Double = p
    def format: String   = f"${p * 100}%.2f%%"
