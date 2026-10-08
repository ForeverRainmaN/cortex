package cortex.domain.auth

import java.util.UUID

opaque type RefreshTokenId = UUID

object RefreshTokenId:
  def apply(value: UUID): RefreshTokenId = value

  extension (id: RefreshTokenId) def value: UUID = id

opaque type AccessToken = String

object AccessToken:
  def apply(value: String): AccessToken = value

  extension (token: AccessToken) def value: String = token

opaque type RefreshToken = String

object RefreshToken:
  def apply(value: String): RefreshToken = value

  extension (token: RefreshToken) def value: String = token

opaque type RefreshTokenHash = String

object RefreshTokenHash:
  def apply(value: String): RefreshTokenHash = value

  extension (hash: RefreshTokenHash) def value: String = hash

final case class TokenPair(
  accessToken: AccessToken,
  refreshToken: RefreshToken
)
