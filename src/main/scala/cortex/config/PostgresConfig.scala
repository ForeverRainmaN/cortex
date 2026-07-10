package cortex.config

import pureconfig.ConfigReader
import pureconfig.generic.derivation.*

final case class PostgresConfig(nThreads: Int, url: String, user: String, password: String) derives ConfigReader
