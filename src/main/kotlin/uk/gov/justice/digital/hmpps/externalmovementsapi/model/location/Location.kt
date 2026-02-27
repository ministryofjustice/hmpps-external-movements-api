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

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as Location

    if (description != other.description) return false
    if (address != other.address) return false
    if (postcode != other.postcode) return false

    return true
  }

  override fun hashCode(): Int = description?.hashCode() ?: ((0 + (address?.hashCode() ?: 0)) + (postcode?.hashCode() ?: 0))

  companion object {
    fun empty(): Location = Location(null, null, null, null)
  }
}

fun Location?.isNullOrEmpty(): Boolean = this == null || isEmpty()
