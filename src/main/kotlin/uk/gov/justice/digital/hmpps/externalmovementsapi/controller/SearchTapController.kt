package uk.gov.justice.digital.hmpps.externalmovementsapi.controller

import jakarta.validation.Valid
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.externalmovementsapi.access.Roles
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.paged.PersonTapSearchRequest
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.paged.PersonTapSearchResponse
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.paged.TapAuthorisationSearchRequest
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.paged.TapAuthorisationSearchResponse
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.paged.TapOccurrenceSearchRequest
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.paged.TapOccurrenceSearchResponse
import uk.gov.justice.digital.hmpps.externalmovementsapi.service.search.SearchTapAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.service.search.SearchTapOccurrence

@RestController
@RequestMapping("/search")
@PreAuthorize("hasRole('${Roles.EXTERNAL_MOVEMENTS_UI}')")
class SearchTapController(
  private val authorisations: SearchTapAuthorisation,
  private val occurrences: SearchTapOccurrence,
) {
  @PostMapping("/temporary-absence-authorisations")
  fun searchTapAuthorisations(@Valid @RequestBody request: TapAuthorisationSearchRequest): TapAuthorisationSearchResponse = authorisations.find(request)

  @PostMapping("/temporary-absence-occurrences")
  fun searchTapOccurrences(@Valid @RequestBody request: TapOccurrenceSearchRequest): TapOccurrenceSearchResponse = occurrences.find(request)

  @PostMapping("/people/{personIdentifier}/temporary-absence-occurrences")
  fun searchPersonTap(
    @PathVariable personIdentifier: String,
    @Valid @RequestBody request: PersonTapSearchRequest,
  ): PersonTapSearchResponse = occurrences.findForPerson(personIdentifier, request)
}
