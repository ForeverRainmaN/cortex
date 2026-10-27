package cortex.api

object syntax:
  opaque type Routes[F[_]] = List[ServerEndpoint[Any, F]]

  object Routes:
    def apply[F[_]](endpoints: List[ServerEndpoint[Any, F]]): Routes[F] = endpoints

    extension [F[_]](routes: Routes[F]) def toList: List[ServerEndpoint[Any, F]] = routes

  extension [F[_]](e: ServerEndpoint[Any, F])
    def ~>(other: ServerEndpoint[Any, F]): Routes[F] =
      Routes(List(e, other))

  extension [F[_]](routes: Routes[F])
    def ~>(other: ServerEndpoint[Any, F]): Routes[F] =
      Routes(routes :+ other)

    def ~>(other: Routes[F]): Routes[F] =
      Routes(routes ::: other)

  type ServerEndpoint[R, F[_]] = sttp.tapir.server.ServerEndpoint[R, F]