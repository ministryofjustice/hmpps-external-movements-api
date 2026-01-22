package uk.gov.justice.digital.hmpps.externalmovementsapi.controller

import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.externalmovementsapi.access.Roles
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.PrisonExternalMovementOverview
import uk.gov.justice.digital.hmpps.externalmovementsapi.service.GetPrisonOverview

@RestController
@RequestMapping("prisons")
@PreAuthorize("hasRole('${Roles.EXTERNAL_MOVEMENTS_UI}')")
class PrisonController(private val overview: GetPrisonOverview) {
  @GetMapping("/{prisonIdentifier}/external-movements/overview")
  fun getPrisonOverview(@PathVariable prisonIdentifier: String): PrisonExternalMovementOverview = overview.forPrison(prisonIdentifier)
}
