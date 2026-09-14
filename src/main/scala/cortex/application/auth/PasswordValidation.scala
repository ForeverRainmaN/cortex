package cortex.application.auth

import java.nio.charset.StandardCharsets.UTF_8

private[auth] def validatePassword(rawPassword: String): Either[String, Unit] =
  Either.cond(
    rawPassword.nonEmpty && rawPassword.getBytes(UTF_8).length <= 72,
    (),
    "Password must contain between 1 and 72 UTF-8 bytes"
  )