scalaVersion := "3.8.4"

lazy val root = rootProject
  .settings(
    name := "cortex",
    libraryDependencies ++= Seq(
      "org.typelevel"         %% "cats-effect"                   % "3.7.0",
      "org.http4s"            %% "http4s-ember-server"           % "0.23.36",
      "org.http4s"            %% "http4s-circe"                  % "0.23.36",
      "org.scalatest"         %% "scalatest"                     % "3.2.20"     % Test,
      "org.typelevel"         %% "cats-effect-testing-scalatest" % "1.8.0"      % Test,
      "org.http4s"            %% "http4s-dsl"                    % "0.23.36",
      "io.circe"              %% "circe-generic"                 % "0.14.16",
      "com.github.pureconfig" %% "pureconfig-core"               % "0.17.10",
      "io.github.jmcardon"    %% "tsec-http4s"                   % "0.5.0",
      "org.tpolecat"          %% "doobie-core"                   % "1.0.0-RC12",
      "org.tpolecat"          %% "doobie-hikari"                 % "1.0.0-RC12",
      "org.tpolecat"          %% "doobie-postgres"               % "1.0.0-RC12",
      "org.tpolecat"          %% "doobie-scalatest"              % "1.0.0-RC12" % Test,
      "org.testcontainers"     % "testcontainers"                % "2.0.5"      % Test,
      "org.testcontainers"     % "postgresql"                    % "1.21.4"     % Test,
      "org.typelevel"         %% "log4cats-slf4j"                % "2.8.0",
      "org.slf4j"              % "slf4j-simple"                  % "2.0.18",
    )
  )
