package cortex.testkit

import cats.Applicative
import cats.effect.{Clock, IO, Ref}

import java.time.Instant
import java.time.temporal.ChronoUnit
import scala.concurrent.duration.*

final class ManualClock private (now: Ref[IO, FiniteDuration]) extends Clock[IO]:
  override def applicative: Applicative[IO]  = Applicative[IO]
  override def monotonic: IO[FiniteDuration] = now.get
  override def realTime: IO[FiniteDuration]  = now.get

  def advance(by: FiniteDuration): IO[Unit] = now.update(_ + by)

object ManualClock:
  def at(start: Instant): IO[ManualClock] =
    Ref
      .of[IO, FiniteDuration](ChronoUnit.NANOS.between(Instant.EPOCH, start).nanos)
      .map(new ManualClock(_))
