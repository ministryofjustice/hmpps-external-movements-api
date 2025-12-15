package uk.gov.justice.digital.hmpps.externalmovementsapi.controller

import jakarta.validation.Valid
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.externalmovementsapi.access.Roles
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.ScheduledMovements
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.SearchScheduledMovementsRequest
import uk.gov.justice.digital.hmpps.externalmovementsapi.service.search.SearchScheduledMovements

@RestController
@PreAuthorize("hasRole('${Roles.EXTERNAL_MOVEMENTS_RO}')")
@RequestMapping("/search/prisons/{prisonCode}/external-movements")
class SearchExternalMovementsController(private val scheduledMovements: SearchScheduledMovements) {
  @GetMapping("/schedules")
  fun externalMovementSchedules(
    @PathVariable prisonCode: String,
    @Valid request: SearchScheduledMovementsRequest,
  ): ScheduledMovements = scheduledMovements.search(prisonCode, request)
}
