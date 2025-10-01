package uk.gov.justice.digital.hmpps.externalmovementsapi.service.locationsearch

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.LocationType
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.organisations.Organisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.organisations.OrganisationsClient
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.organisations.PagedOrganisations
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.paged.LocationResult
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.paged.LocationSearchRequest
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.paged.LocationSearchResponse
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.paged.PageMetadata

interface LocationSearch {
  val applicableTypes: Set<LocationType.Code>
  fun search(searchRequest: LocationSearchRequest): LocationSearchResponse
}

@Component
class CorporationSearch(private val organisations: OrganisationsClient) : LocationSearch {
  override val applicableTypes = setOf(LocationType.Code.CORP)
  override fun search(searchRequest: LocationSearchRequest): LocationSearchResponse = organisations.findOrganisations(searchRequest).asLocationSearchResponse()

  private fun Organisation.asLocation() = LocationResult(
    organisationName,
    listOfNotNull(flat, property).takeIf { it.isNotEmpty() }?.joinToString(", "),
    street,
    area,
    cityDescription,
    countyDescription,
    countryDescription,
    postcode,
  )

  private fun PagedOrganisations.asLocationSearchResponse() = LocationSearchResponse(
    content.map { it.asLocation() },
    page,
  )
}

@Component
class EmptySearch : LocationSearch {
  override val applicableTypes = setOf(LocationType.Code.AGY, LocationType.Code.OFF)
  override fun search(searchRequest: LocationSearchRequest): LocationSearchResponse = LocationSearchResponse(emptyList(), PageMetadata(0))
}
