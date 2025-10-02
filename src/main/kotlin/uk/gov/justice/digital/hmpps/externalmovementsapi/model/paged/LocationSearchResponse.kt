package uk.gov.justice.digital.hmpps.externalmovementsapi.model.paged

data class LocationSearchResponse(
  override val content: List<LocationResult>,
  override val metadata: PageMetadata,
) : PagedResponse<LocationResult>

data class LocationResult(
  val id: String,
  val name: String?,
  val premise: String?,
  val street: String?,
  val area: String?,
  val city: String?,
  val county: String?,
  val country: String?,
  val postcode: String?,
)
