package cortex.domain.users

import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID
import scala.util.Try

final case class User(
  id: UserId,
  email: Email,
  hashedPassword: HashedPassword,
  createdAt: Instant,
)

object User:
  def make(id: UserId, email: Email, password: HashedPassword, now: Instant): User =
    User(id, email, password, now.truncatedTo(ChronoUnit.MICROS))

opaque type UserId = UUID
object UserId:
  private def apply(id: UUID): UserId = id
  
  def fromUUID(id: UUID): UserId = apply(id)
  
  def fromString(s: String): Either[Throwable, UserId] =
    Try(UUID.fromString(s)).toEither.map(apply)

  extension (userId: UserId) def value: UUID = userId

opaque type Email = String
object Email:
  private def apply(s: String): Email = s
  
  def fromString(v: String): Either[String, Email] =
    if v.contains("@") && v.contains(".") then Right(apply(v))
    else Left(s"Invalid email address: $v")
  
  def fromStringUnsafe(v: String): Email = apply(v)

  extension (e: Email)
    def value: String  = e
    def domain: String = e.substring(e.indexOf("@") + 1)

opaque type HashedPassword = String

object HashedPassword:
  private def apply(v: String): HashedPassword = v
  
  def fromHash(v: String): HashedPassword = apply(v)

  extension (p: HashedPassword) def value: String = p
