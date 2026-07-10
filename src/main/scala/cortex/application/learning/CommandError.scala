package cortex.application.learning

import cortex.domain.learning.DecideError

enum CommandError:
  case Validation(error: DecideError)
  case Storage(cause: Throwable)
