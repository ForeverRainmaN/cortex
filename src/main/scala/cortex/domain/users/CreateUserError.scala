package cortex.domain.users

enum CreateUserError:
  case EmailAlreadyExists(email: Email)
