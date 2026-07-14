package cortex

import cats.effect.kernel.Async
import cats.effect.{IO, IOApp, Resource}
import cortex.config.PostgresConfig
import cortex.config.syntax.*
import doobie.*
import doobie.hikari.HikariTransactor
import doobie.implicits.*
import doobie.util.ExecutionContexts
import doobie.util.log.LogHandler
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import pureconfig.ConfigSource

object Application extends IOApp.Simple:
  given logger: Logger[IO]   = Slf4jLogger.getLogger[IO]
  override def run: IO[Unit] =
    val config = ConfigSource.default
      .at("postgres-config")
      .loadF[IO, PostgresConfig].flatMap: config =>
        createTransactor[IO](config).use { xa =>
          sql"SELECT 1".query[Int].unique.transact(xa).flatMap { result =>
            IO.println(s"DB connection OK: $result")
          }
        }
    config >> IO.println("done!")

  private def createTransactor[F[_]: Async](config: PostgresConfig): Resource[F, HikariTransactor[F]] =
    for
      ec <- ExecutionContexts.fixedThreadPool(config.nThreads)
      xa <- HikariTransactor.newHikariTransactor[F](
              "org.postgresql.Driver",
              config.url,
              config.user,
              config.password,
              ec,
              Some(LogHandler.jdkLogHandler[F])
            )
    yield xa
