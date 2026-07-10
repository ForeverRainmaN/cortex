package cortex.config

import pureconfig.ConfigReader
import pureconfig.generic.derivation.*

import scala.concurrent.duration.FiniteDuration

final case class SecurityConfig(secret: String, jwtExpiryDuration: FiniteDuration) derives ConfigReader
