package cortex.syntax

import java.util.UUID

extension (s: String) def toUUID: UUID = UUID.fromString(s)