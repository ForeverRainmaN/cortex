package cortex.application.auth

import cortex.domain.users.Email

enum RegisterError:
  case InvalidEmail(message: String)
  case InvalidPassword(message: String)
  case EmailAlreadyExists(email: Email)
