package uk.gov.justice.digital.hmpps.externalmovementsapi.controller

import jakarta.validation.Valid
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.externalmovementsapi.access.Roles
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.LocationType
import uk.gov.justice.digital.hmpps.externalmovementsapi.exception.NotFoundException
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.Location
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.paged.LocationSearchRequest
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.paged.LocationSearchResponse
import uk.gov.justice.digital.hmpps.externalmovementsapi.service.locationsearch.GetLocations

@RestController
@RequestMapping
@PreAuthorize("hasRole('${Roles.EXTERNAL_MOVEMENTS_UI}')")
class LocationController(private val locations: GetLocations) {
  @GetMapping("search/locations/{typeCode}")
  fun findLocations(
    @PathVariable typeCode: LocationType.Code,
    @Valid request: LocationSearchRequest,
  ): LocationSearchResponse = locations.find(typeCode, request)

  @GetMapping("locations/{typeCode}/{id}")
  fun findLocationById(
    @PathVariable typeCode: LocationType.Code,
    @PathVariable id: String,
  ): Location = locations.withId(typeCode, id) ?: throw NotFoundException("Location not found")
}
