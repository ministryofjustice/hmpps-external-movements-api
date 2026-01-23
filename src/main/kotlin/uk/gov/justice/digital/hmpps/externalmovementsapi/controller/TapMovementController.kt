package uk.gov.justice.digital.hmpps.externalmovementsapi.controller

import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.externalmovementsapi.access.Roles
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.AuditHistory
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.TapMovement
import uk.gov.justice.digital.hmpps.externalmovementsapi.service.GetTapMovement
import uk.gov.justice.digital.hmpps.externalmovementsapi.service.history.MovementHistory
import java.util.UUID

@RestController
@RequestMapping("/temporary-absence-movements")
@PreAuthorize("hasRole('${Roles.EXTERNAL_MOVEMENTS_UI}')")
class TapMovementController(
  private val get: GetTapMovement,
  private val history: MovementHistory,
) {
  @GetMapping("/{id}")
  fun getTapMovement(@PathVariable id: UUID): TapMovement = get.byId(id)

  @GetMapping("/{id}/history")
  fun getTapMovementHistory(@PathVariable id: UUID): AuditHistory = history.changes(id)
}
