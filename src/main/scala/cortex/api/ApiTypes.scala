package cortex.api

import sttp.tapir.server.ServerEndpoint

type SimpleRoute[F[_]] = ServerEndpoint[Any, F]
