package cortex.application.auth

enum LoginError:
  case InvalidEmail(message: String)
  case InvalidPassword(message: String)
  case InvalidCredentials
