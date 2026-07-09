scalaVersion := "3.8.4"

lazy val root = rootProject
  .settings(
    name := "cortex",
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-effect"                   % "3.7.0",
      "org.http4s"    %% "http4s-ember-server"           % "0.23.36",
      "org.http4s"    %% "http4s-circe"                  % "0.23.36",
      "org.scalatest" %% "scalatest"                     % "3.2.20" % Test,
      "org.typelevel" %% "cats-effect-testing-scalatest" % "1.8.0"  % Test,
      "org.http4s"    %% "http4s-dsl"                    % "0.23.36",
      "io.circe"      %% "circe-generic"                 % "0.14.16",
      "org.tpolecat"  %% "doobie-core"                   % "1.0.0-RC12",
      "org.tpolecat"  %% "doobie-postgres"               % "1.0.0-RC12",
      "is.cir"        %% "ciris"                         % "3.15.0"
    )
  )
