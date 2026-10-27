import sbt.*
import Keys.*

val colimaDockerEnv: Map[String, String] =
  val socket = file(sys.props("user.home")) / ".colima" / "default" / "docker.sock"
  if !sys.env.contains("DOCKER_HOST") && socket.exists then
    Map(
      "DOCKER_HOST" -> s"unix://${socket.getAbsolutePath}",
      "TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE" -> "/var/run/docker.sock"
    )
  else Map.empty

scalaVersion := "3.10.0"

lazy val root = rootProject
  .settings(
    name := "cortex",
    Test / fork := true,
    Test / envVars ++= colimaDockerEnv,
    Compile / scalacOptions += "-language:postfixOps",
    Test / scalacOptions += "-language:postfixOps",
    Test / parallelExecution := false,
    libraryDependencies ++= Seq(
      "org.typelevel"               %% "cats-effect"                   % "3.7.1",
      "org.http4s"                  %% "http4s-ember-server"           % "0.23.38",
      "org.http4s"                  %% "http4s-circe"                  % "0.23.38",
      "org.scalatest"               %% "scalatest"                     % "3.2.20" % Test,
      "org.typelevel"               %% "cats-effect-testing-scalatest" % "1.8.0" % Test,
      "org.http4s"                  %% "http4s-dsl"                    % "0.23.38",
      "io.circe"                    %% "circe-generic"                 % "0.14.17",
      "com.github.pureconfig"       %% "pureconfig-core"               % "0.17.10",
      "io.github.jmcardon"          %% "tsec-http4s"                   % "0.6.0",
      "org.tpolecat"                %% "doobie-core"                   % "1.0.0-RC12",
      "org.tpolecat"                %% "doobie-hikari"                 % "1.0.0-RC12",
      "org.tpolecat"                %% "doobie-postgres"               % "1.0.0-RC12",
      "org.tpolecat"                %% "doobie-scalatest"              % "1.0.0-RC12" % Test,
      "org.testcontainers"          % "testcontainers"                % "1.21.4" % Test,
      "org.testcontainers"          % "postgresql"                    % "1.21.4" % Test,
      "org.typelevel"               %% "log4cats-slf4j"                % "2.8.0",
      "org.slf4j"                    % "slf4j-simple"                  % "2.0.20",
      "com.softwaremill.sttp.tapir" %% "tapir-http4s-server"           % "1.14.0",
      "com.softwaremill.sttp.tapir" %% "tapir-json-circe"              % "1.14.0",
      "com.softwaremill.sttp.tapir" %% "tapir-swagger-ui"              % "1.14.0",
      "com.github.jwt-scala"        %% "jwt-circe"                     % "12.0.0"
    )
  )
