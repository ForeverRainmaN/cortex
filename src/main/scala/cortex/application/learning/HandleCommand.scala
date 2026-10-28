package cortex.application.learning

import cats.{Functor, Monad}
import cats.data.EitherT
import cats.syntax.either.given
import cortex.domain.learning.*

def handleCommand[F[_]: Monad](store: EventStore[F])(id: ContentId, command: Command): F[Either[CommandError, Unit]] =
  (for
    eventHistory  <- EitherT.liftF(store.loadById(id))
    currentState   = eventHistory.events.foldLeft(Option.empty[ContentState])(evolve)
    currentVersion = eventHistory.version
    event         <- EitherT.fromEither[F](decide(id, currentState, command).leftMap(CommandError.Validation(_)))
    _             <- handleAppend(store, event, currentVersion)
  yield ()).value

def handleAppend[F[_]: Functor](
  store: EventStore[F],
  event: LearningEvent,
  currentVersion: EventVersion
): EitherT[F, CommandError, EventVersion] =
  EitherT(store.append(event, currentVersion)).leftMap:
    case AppendError.VersionConflict(expected, actual) =>
      CommandError.Conflict(expected, actual)

    case AppendError.VersionOverflow =>
      CommandError.VersionOverflow
