package cortex.domain.curriculum

enum PathSectionError:
  case DuplicatePathItems(pathItemIds: Set[PathItemId])
  case EmptyTitle
