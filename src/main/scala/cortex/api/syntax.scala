package cortex.api

object syntax:
  type ServerEndpoint[R, F[_]] = sttp.tapir.server.ServerEndpoint[R, F]
  type Route[F[_]] = ServerEndpoint[Any, F]

  opaque type Routes[F[_]] = List[Route[F]]

  object Routes:
    def apply[F[_]](endpoints: List[Route[F]]): Routes[F] = endpoints

    extension [F[_]](routes: Routes[F]) def toList: List[Route[F]] = routes

  extension [F[_]](e: Route[F])
    def ~>(other: Route[F]): Routes[F] =
      Routes(List(e, other))

  extension [F[_]](routes: Routes[F])
    def ~>(other: Route[F]): Routes[F] =
      Routes(routes :+ other)

    def ~>(other: Routes[F]): Routes[F] =
      Routes(routes ::: other)
