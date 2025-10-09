package uk.gov.justice.digital.hmpps.externalmovementsapi.service.locationsearch

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.LocationType
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.Location
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.paged.LocationSearchRequest
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.paged.LocationSearchResponse

@Service
class GetLocations(locationSearches: List<LocationSearch>) {
  private val locationServiceMap = locationSearches.flatMap { s -> s.applicableTypes.map { t -> t to s } }.toMap()
  fun find(typeCode: LocationType.Code, request: LocationSearchRequest): LocationSearchResponse = locationServiceMap[typeCode]?.search(request) ?: throw IllegalArgumentException("Location type not searchable")

  fun withId(typeCode: LocationType.Code, id: String): Location? = locationServiceMap[typeCode]?.byId(typeCode, id)
  fun withIds(ids: Map<LocationType.Code, List<String>>): List<Location> = ids.entries.mapNotNull {
    locationServiceMap[it.key]?.byIds(it.key, it.value.toSet())
  }.flatten()
}
