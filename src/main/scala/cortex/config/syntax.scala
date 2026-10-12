package cortex.config

import cats.implicits.*
import cats.effect.kernel.Sync
import pureconfig.error.ConfigReaderException
import pureconfig.{ConfigReader, ConfigSource}

import scala.reflect.ClassTag

object syntax:
  extension (source: ConfigSource)
    def loadF[F[_], A](using reader: ConfigReader[A], F: Sync[F], ct: ClassTag[A]): F[A] =
      F.blocking(source.load[A])
        .flatMap(_.leftMap(ConfigReaderException[A](_)).liftTo[F])
