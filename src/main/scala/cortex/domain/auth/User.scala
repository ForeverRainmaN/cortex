package cortex.domain.auth

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
  def apply(id: UUID): UserId                          = id
  def fromString(s: String): Either[Throwable, UserId] =
    Try(UUID.fromString(s)).toEither

  def generate: UserId =
    UUID.randomUUID()

  extension (userId: UserId) def value: UUID = userId

opaque type Email = String
object Email:
  def apply(s: String): Email                      = s
  def fromString(v: String): Either[String, Email] =
    if v.contains("@") && v.contains(".") then Right(v)
    else Left(s"Invalid email address: $v")

  extension (e: Email)
    def value: String  = e
    def domain: String = e.substring(e.indexOf("@") + 1)

opaque type HashedPassword = String

object HashedPassword:
  def apply(v: String): HashedPassword = v

  extension (p: HashedPassword) def value: String = p
