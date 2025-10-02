package uk.gov.justice.digital.hmpps.externalmovementsapi.integration.organisations

import uk.gov.justice.digital.hmpps.externalmovementsapi.model.paged.PageMetadata

data class Organisation(
  val organisationId: Long,
  val organisationName: String,
  val flat: String?,
  val property: String?,
  val street: String?,
  val area: String?,
  val cityCode: String?,
  val cityDescription: String?,
  val countyCode: String?,
  val countyDescription: String?,
  val postcode: String?,
  val countryCode: String?,
  val countryDescription: String?,
)

data class PagedOrganisations(val content: List<Organisation>, val page: PageMetadata)
