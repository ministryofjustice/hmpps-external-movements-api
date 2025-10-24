package uk.gov.justice.digital.hmpps.externalmovementsapi.model.location

@ValidLocation
data class Location(
  val description: String?,
  val address: String?,
  val postcode: String?,
  val uprn: String?,
) {
  companion object {
    fun unknown() = Location("Location not found", null, null, null)
  }
}
