package cortex.domain.curriculum

import cortex.domain.catalog.ResourceId

enum LearningPlanError:
  case EmptyTitle
  case DuplicateItems(itemIds: Set[PlanItemId])
  case DuplicateResources(resourceIds: Set[ResourceId])
