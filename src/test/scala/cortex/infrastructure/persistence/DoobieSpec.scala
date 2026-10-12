package cortex.infrastructure.persistence

import cats.effect.*
import doobie.hikari.HikariTransactor
import doobie.{ExecutionContexts, Transactor}
import org.testcontainers.containers.PostgreSQLContainer

trait DoobieSpec:
  private val schemaScript = "db/schema.sql"

  val postgres: Resource[IO, PostgreSQLContainer[Nothing]] =
    val acquire = IO:
      val container: PostgreSQLContainer[Nothing] = new PostgreSQLContainer("postgres").withInitScript(schemaScript)
      container.start()
      container

    val release = (container: PostgreSQLContainer[Nothing]) => IO(container.stop())
    Resource.make(acquire)(release)

  val transactor: Resource[IO, Transactor[IO]] =
    for
      db <- postgres
      ce <- ExecutionContexts.fixedThreadPool[IO](1)
      xa <- HikariTransactor.newHikariTransactor[IO](
              "org.postgresql.Driver",
              db.getJdbcUrl,
              db.getUsername,
              db.getPassword,
              ce
            )
    yield xa

  def withRepository[R, A](
    makeRepository: Transactor[IO] => R
  )(test: R => IO[A]): IO[A] =
    transactor.use: xa =>
      test(makeRepository(xa))
