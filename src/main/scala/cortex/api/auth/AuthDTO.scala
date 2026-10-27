package cortex.api.auth

object AuthDTO:
  final case class Credentials(
    email: String,
    password: String
  )

  final case class SignUpResponse(
    userId: String,
    email: String
  )

  final case class LoginResponse(
    accessToken: String,
    refreshToken: String
  )

  final case class ErrorResponse(
    code: String,
    message: String
  )
