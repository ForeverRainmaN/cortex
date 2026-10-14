package cortex.domain.curriculum

import cortex.domain.catalog.ResourceId

enum LearningPathError:
  case EmptyTitle
  case EmptySectionTitle(sectionIds: Set[SectionId])
  case DuplicateSections(sectionIds: Set[SectionId])
  case DuplicateItems(itemIds: Set[PathItemId])
  case DuplicateResources(resourceIds: Set[ResourceId])
  case SectionNotFound(sectionId: SectionId)
  case InvalidSection(error: PathSectionError)
