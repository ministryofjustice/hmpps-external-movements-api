package uk.gov.justice.digital.hmpps.externalmovementsapi.model

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
  )

  companion object {
    fun unknown() = Location("Location not found", null, null)
  }
}
