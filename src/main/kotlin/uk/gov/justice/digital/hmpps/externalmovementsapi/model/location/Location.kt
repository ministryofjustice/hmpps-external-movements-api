package uk.gov.justice.digital.hmpps.externalmovementsapi.model.location

import com.fasterxml.jackson.annotation.JsonIgnore

@ValidLocation
data class Location(
  val description: String?,
  val address: String?,
  val postcode: String?,
  val uprn: Long?,
) {
  @JsonIgnore
  fun isEmpty(): Boolean = description.isNullOrBlank() && address.isNullOrBlank() && postcode.isNullOrBlank()
  override fun toString(): String = listOfNotNull(description?.trim(), address?.trim(), postcode?.trim()).joinToString()

  companion object {
    fun empty(): Location = Location(null, null, null, null)
  }
}

fun Location?.isNullOrEmpty(): Boolean = this == null || isEmpty()
