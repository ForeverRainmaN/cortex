package cortex.config

import pureconfig.ConfigReader

import scala.concurrent.duration.FiniteDuration

final case class SecurityConfig(secret: String, jwtExpiryDuration: FiniteDuration) derives ConfigReader
