package cortex.auth

import cortex.auth.Model.Email
import java.util.UUID

opaque type UserId = UUID

object UserId:
  def apply(u: UUID): UserId        = u
  def fromString(s: String): UserId = UUID.fromString(s)

final case class User(
  userId: UserId,
  email: Email,
  hashedPassword: String,
  firstName: Option[String],
  lastName: Option[String]
)

object Model:
  opaque type Email = String

  object Email:
    def fromString(value: String): Either[String, Email] =
      if value.contains("@") && value.contains(".") then Right(value)
      else Left(s"Invalid email address: $value")

    extension (e: Email)
      def value: String  = e
      def domain: String = e.substring(e.indexOf("@") + 1)
