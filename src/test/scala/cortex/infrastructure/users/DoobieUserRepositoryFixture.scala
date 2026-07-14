package cortex.infrastructure.users

import cortex.domain.users.{Email, HashedPassword, User, UserId}

import java.time.Instant

trait DoobieUserRepositoryFixture:
  val testUser: User =
    User.make(
      id = UserId.generate,
      email = Email("testEmail@gmail.com"),
      password = HashedPassword("$2a$10$jY60jL/9Lv6./UHhhj2ZvOSm8PQIiTueC4gmsegrD5K.Yi6/mGY.m"),
      now = Instant.now()
    )
