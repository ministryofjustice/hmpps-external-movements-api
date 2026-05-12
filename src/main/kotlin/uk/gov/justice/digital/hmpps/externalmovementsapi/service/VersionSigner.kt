package uk.gov.justice.digital.hmpps.externalmovementsapi.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.VersionToken
import java.security.MessageDigest
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

@Component
class VersionSigner(
  @Value($$"${service.version.key}") private val versionKey: String,
) {
  fun generateToken(id: String, version: Int): VersionToken {
    val signature = calculateHmac("$id:$version")
    return VersionToken(version, signature)
  }

  fun verifyToken(token: VersionToken, id: String): Boolean {
    val expectedSig = calculateHmac("$id:${token.number}")
    return MessageDigest.isEqual(token.signature.toByteArray(), expectedSig.toByteArray())
  }

  private fun calculateHmac(data: String): String {
    val mac = Mac.getInstance(HMAC_ALGO)
    val keySpec = SecretKeySpec(versionKey.toByteArray(), HMAC_ALGO)
    mac.init(keySpec)
    val rawHmac = mac.doFinal(data.toByteArray())
    return Base64.getUrlEncoder()
      .withoutPadding()
      .encodeToString(rawHmac)
      .take(SIGNATURE_LENGTH)
  }

  companion object {
    private const val HMAC_ALGO = "HmacSHA256"
    private const val SIGNATURE_LENGTH = 10
  }
}
