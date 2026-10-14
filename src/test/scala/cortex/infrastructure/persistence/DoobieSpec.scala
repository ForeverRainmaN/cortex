package cortex.infrastructure.persistence

import cats.effect.*
import cats.effect.unsafe.implicits.global
import doobie.hikari.HikariTransactor
import doobie.implicits.*
import doobie.{ExecutionContexts, Transactor}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Suite}
import org.testcontainers.DockerClientFactory
import org.testcontainers.containers.PostgreSQLContainer

trait DoobieSpec extends BeforeAndAfterAll, BeforeAndAfterEach:
  self: Suite =>

  private val postgresImage = "postgres:16-alpine"
  private val schemaScript  = "db/schema.sql"
  private val tables        = List("refresh_tokens", "users", "events")

  private val dockerAvailable: Boolean =
    scala.util.Try(DockerClientFactory.instance().isDockerAvailable).getOrElse(false)

  private var session: Option[(Transactor[IO], IO[Unit])] = None

  protected def transactor: Transactor[IO] =
    session.map(_._1).getOrElse(cancel("Docker is not available"))

  override protected def beforeAll(): Unit =
    super.beforeAll()
    if dockerAvailable then
      val (xa, release) = transactorResource.allocated.unsafeRunSync()
      session = Some((xa, release))

  override protected def afterAll(): Unit =
    session.foreach((_, release) => release.unsafeRunSync())
    session = None
    super.afterAll()

  override protected def beforeEach(): Unit =
    super.beforeEach()
    session.foreach((xa, _) => truncateAll(xa).unsafeRunSync())

  protected def withRepository[R, A](makeRepository: Transactor[IO] => R)(test: R => IO[A]): IO[A] =
    test(makeRepository(transactor))

  private def truncateAll(xa: Transactor[IO]): IO[Unit] =
    doobie.Fragment
      .const(s"TRUNCATE TABLE ${tables.mkString(", ")} RESTART IDENTITY CASCADE")
      .update
      .run
      .transact(xa)
      .void

  private def transactorResource: Resource[IO, Transactor[IO]] =
    for
      db <- postgres
      ce <- ExecutionContexts.fixedThreadPool[IO](4)
      xa <- HikariTransactor.newHikariTransactor[IO](
              "org.postgresql.Driver",
              db.getJdbcUrl,
              db.getUsername,
              db.getPassword,
              ce
            )
    yield xa

  private def postgres: Resource[IO, PostgreSQLContainer[Nothing]] =
    val acquire = IO.blocking:
      val container: PostgreSQLContainer[Nothing] =
        new PostgreSQLContainer(postgresImage).withInitScript(schemaScript)
      container.start()
      container

    Resource.make(acquire)(container => IO.blocking(container.stop()))
