package cortex.config

import pureconfig.ConfigReader

final case class CortexConfig(emberConfig: EmberConfig, postgresConfig: PostgresConfig, securityConfig: SecurityConfig)
  derives ConfigReader
