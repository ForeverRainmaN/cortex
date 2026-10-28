package cortex.application.learning

import cortex.domain.learning.{DecideError, EventVersion}

enum CommandError:
  case Validation(error: DecideError)
  case Storage(cause: Throwable)
  case Conflict(expected: EventVersion, actual: EventVersion)
  case VersionOverflow
