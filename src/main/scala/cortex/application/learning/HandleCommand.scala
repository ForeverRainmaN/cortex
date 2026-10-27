package cortex.application.learning

import cats.Monad
import cats.data.EitherT
import cats.syntax.either.given
import cortex.domain.learning.*

def handleCommand[F[_]: Monad](store: EventStore[F])(id: ContentId, command: Command): F[Either[CommandError, Unit]] =
  (for
    events <- EitherT.liftF(store.loadById(id))
    state   = events.foldLeft(Option.empty[ContentState])(evolve)
    event  <- EitherT.fromEither[F](decide(id, state, command).leftMap(CommandError.Validation(_)))
    _      <- EitherT.liftF(store.append(event))
  yield ()).value
