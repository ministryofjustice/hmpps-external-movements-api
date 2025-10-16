package uk.gov.justice.digital.hmpps.externalmovementsapi.model.location

import com.fasterxml.jackson.annotation.JsonIgnore

@ValidLocation
data class Location(
  val description: String?,
  val address: Address?,
  val id: String?,
) {

  data class Address(
    val premise: String?,
    val street: String?,
    val area: String?,
    val city: String?,
    val county: String?,
    val country: String?,
    val postcode: String?,
  ) {
    @JsonIgnore
    fun isEmpty() = listOfNotNull(
      premise,
      street,
      area,
      city,
      county,
      country,
      postcode,
    ).isEmpty()
  }

  companion object {
    fun unknown() = Location("Location not found", null, null)
  }
}
