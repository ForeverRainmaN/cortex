package cortex.domain.auth

import java.util.UUID

opaque type RefreshTokenId = UUID

object RefreshTokenId:
  private def apply(value: UUID): RefreshTokenId = value
  
  def fromUUID(value: UUID): RefreshTokenId = apply(value)

  extension (id: RefreshTokenId) def value: UUID = id

opaque type AccessToken = String

object AccessToken:
  private def apply(value: String): AccessToken = value
  
  def fromString(value: String): AccessToken = apply(value)

  extension (token: AccessToken) def value: String = token

opaque type RefreshToken = String

object RefreshToken:
  private def apply(value: String): RefreshToken = value
  
  def fromString(value: String): RefreshToken = apply(value)

  extension (token: RefreshToken) def value: String = token

opaque type RefreshTokenHash = String

object RefreshTokenHash:
  private def apply(value: String): RefreshTokenHash = value
  
  def fromHash(value: String): RefreshTokenHash = apply(value)

  extension (hash: RefreshTokenHash) def value: String = hash

final case class TokenPair(
  accessToken: AccessToken,
  refreshToken: RefreshToken
)
