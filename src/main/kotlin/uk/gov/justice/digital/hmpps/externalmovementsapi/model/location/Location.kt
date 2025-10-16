package uk.gov.justice.digital.hmpps.externalmovementsapi.model.location

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
    fun isEmpty() = listOfNotNull(
      premise,
      street,
      area,
      city,
      country,
      postcode,
    ).isEmpty()
  }

  companion object {
    fun unknown() = Location("Location not found", null, null)
  }
}
