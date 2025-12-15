package uk.gov.justice.digital.hmpps.externalmovementsapi.controller

import jakarta.validation.Valid
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.externalmovementsapi.access.Roles
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.paged.TapAuthorisationSearchRequest
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.paged.TapAuthorisationSearchResponse
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.paged.TapOccurrenceSearchRequest
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.paged.TapOccurrenceSearchResponse
import uk.gov.justice.digital.hmpps.externalmovementsapi.service.search.SearchTapAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.service.search.SearchTapOccurrence

@RestController
@RequestMapping("/search")
@PreAuthorize("hasAnyRole('${Roles.EXTERNAL_MOVEMENTS_UI}', '${Roles.EM_UI_TEMP}')")
class SearchTapController(private val authorisations: SearchTapAuthorisation, private val occurrences: SearchTapOccurrence) {
  @GetMapping("/temporary-absence-authorisations")
  fun findTapAuthorisations(@Valid request: TapAuthorisationSearchRequest): TapAuthorisationSearchResponse = authorisations.find(request)

  @GetMapping("/temporary-absence-occurrences")
  fun findTapOccurrences(@Valid request: TapOccurrenceSearchRequest): TapOccurrenceSearchResponse = occurrences.find(request)
}
