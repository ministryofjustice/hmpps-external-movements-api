package uk.gov.justice.digital.hmpps.externalmovementsapi.service.locationsearch

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.LocationType
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.paged.LocationSearchRequest
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.paged.LocationSearchResponse

@Service
class SearchLocations(locationSearches: List<LocationSearch>) {
  private val locationSearchMap = locationSearches.flatMap { s -> s.applicableTypes.map { t -> t to s } }.toMap()
  fun find(typeCode: LocationType.Code, request: LocationSearchRequest): LocationSearchResponse = locationSearchMap[typeCode]?.search(request) ?: throw IllegalArgumentException("Location type not searchable")
}
