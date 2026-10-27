package cortex.domain.learning

enum AppendError:
  case VersionConflict(expected: EventVersion, actual: EventVersion)
  case VersionOverflow
