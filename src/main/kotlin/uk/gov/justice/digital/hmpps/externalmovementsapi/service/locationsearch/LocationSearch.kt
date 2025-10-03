package uk.gov.justice.digital.hmpps.externalmovementsapi.service.locationsearch

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.LocationType
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.ReferenceDataDomain
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.ReferenceDataRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.of
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.organisations.Organisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.organisations.OrganisationsClient
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.organisations.PagedOrganisations
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.organisations.asLocation
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.Location
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.paged.LocationResult
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.paged.LocationSearchRequest
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.paged.LocationSearchResponse
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.paged.PageMetadata

interface LocationSearch {
  val applicableTypes: Set<LocationType.Code>
  fun search(searchRequest: LocationSearchRequest): LocationSearchResponse
  fun byId(typeCode: LocationType.Code, id: String): Location?
  fun byIds(typeCode: LocationType.Code, ids: Set<String>): List<Location>
}

@Component
class CorporationSearch(
  private val organisations: OrganisationsClient,
  private val referenceDataRepository: ReferenceDataRepository,
) : LocationSearch {
  override val applicableTypes = setOf(LocationType.Code.CORP)
  override fun search(searchRequest: LocationSearchRequest): LocationSearchResponse = organisations.findOrganisations(searchRequest).asLocationSearchResponse()

  override fun byId(typeCode: LocationType.Code, id: String): Location? {
    val type = checkNotNull(referenceDataRepository.findByKey(ReferenceDataDomain.Code.LOCATION_TYPE of typeCode.name))
    return organisations.getById(id)?.asLocation(type)
  }

  override fun byIds(typeCode: LocationType.Code, ids: Set<String>): List<Location> {
    val type = checkNotNull(referenceDataRepository.findByKey(ReferenceDataDomain.Code.LOCATION_TYPE of typeCode.name))
    return organisations.getByIds(ids).map { it.asLocation(type) }
  }

  private fun Organisation.asResult() = LocationResult(
    "$organisationId",
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
    content.map { it.asResult() },
    page,
  )
}

@Component
class EmptySearch : LocationSearch {
  override val applicableTypes = setOf(LocationType.Code.AGY, LocationType.Code.OFF, LocationType.Code.OTHER)
  override fun search(searchRequest: LocationSearchRequest): LocationSearchResponse = LocationSearchResponse(emptyList(), PageMetadata(0))

  override fun byId(typeCode: LocationType.Code, id: String): Location? = null
  override fun byIds(typeCode: LocationType.Code, ids: Set<String>): List<Location> = emptyList()
}
