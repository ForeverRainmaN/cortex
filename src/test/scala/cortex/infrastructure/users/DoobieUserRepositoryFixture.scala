package cortex.infrastructure.users

import cortex.domain.users.{Email, HashedPassword, User, UserId}

import java.time.Instant
import java.util.UUID

trait DoobieUserRepositoryFixture:
  protected val referenceTime: Instant =
    Instant.parse("2026-10-07T12:00:00Z")

  protected val user: User =
    User.make(
      id = UserId(UUID.fromString("00000000-0000-0000-0000-000000000001")),
      email = Email("testEmail@gmail.com"),
      password = HashedPassword("$2a$10$jY60jL/9Lv6./UHhhj2ZvOSm8PQIiTueC4gmsegrD5K.Yi6/mGY.m"),
      now = referenceTime
    )
