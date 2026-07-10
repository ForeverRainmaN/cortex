package cortex.application.learning

import cats.data.EitherT
import cats.effect.kernel.Sync
import cats.syntax.either.given
import cortex.domain.learning.*
import cortex.infrastructure.learning.EventStore

def handleCommand[F[_]: Sync](store: EventStore[F])(id: ContentId, command: Command): F[Either[CommandError, Unit]] =
  val result: EitherT[F, CommandError, Unit] = for
    events <- EitherT.liftF(store.loadById(id))
    state   = events.foldLeft(Option.empty[ContentState])(evolve)
    event  <- EitherT.fromEither[F](decide(state, command).leftMap(CommandError.Validation(_)))
    _      <- EitherT.liftF(store.append(event))
  yield ()
  result.value
