package uk.gov.justice.digital.hmpps.externalmovementsapi.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

data class VersionToken(
  val number: Int,
  val signature: String,
) {
  @JsonValue
  override fun toString(): String = "$number.$signature"

  companion object {
    @JvmStatic
    @JsonCreator
    fun fromString(token: String): VersionToken {
      val parts = token.split(".")
      require(parts.size == 2) { "Invalid version token format" }
      return VersionToken(
        number = requireNotNull(parts[0].toIntOrNull()) { "Invalid version number" },
        signature = parts[1],
      )
    }
  }
}
