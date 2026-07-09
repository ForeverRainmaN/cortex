package cortex.application

import cortex.domain.DecideError

enum CommandError:
  case Validation(error: DecideError)
  case Storage(cause: Throwable)
