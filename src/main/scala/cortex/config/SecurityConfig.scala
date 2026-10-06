package cortex.config

import pureconfig.ConfigReader

import scala.concurrent.duration.FiniteDuration

final case class SecurityConfig(
  jwtSecret: String,
  accessTokenExpiry: FiniteDuration,
  refreshTokenExpiry: FiniteDuration
) derives ConfigReader

