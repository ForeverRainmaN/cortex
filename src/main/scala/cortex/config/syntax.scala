package cortex.config

import cats.MonadThrow
import cats.syntax.flatMap.*
import pureconfig.error.ConfigReaderException
import pureconfig.{ConfigReader, ConfigSource}

import scala.reflect.ClassTag

object syntax:
  extension (source: ConfigSource)
    def loadF[F[_], A](using reader: ConfigReader[A], F: MonadThrow[F], ct: ClassTag[A]): F[A] =
      F.pure(source.load[A]).flatMap:
          case Left(failures) => F.raiseError[A](ConfigReaderException(failures))
          case Right(config)  => F.pure(config)
