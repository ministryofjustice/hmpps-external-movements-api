package uk.gov.justice.digital.hmpps.externalmovementsapi.sync

import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.externalmovementsapi.access.Roles
import uk.gov.justice.digital.hmpps.externalmovementsapi.sync.internal.SyncScheduledTemporaryAbsence
import uk.gov.justice.digital.hmpps.externalmovementsapi.sync.internal.SyncTapApplication
import uk.gov.justice.digital.hmpps.externalmovementsapi.sync.internal.SyncTapMovement
import java.util.UUID

@Tag(name = "Sync")
@RestController
@RequestMapping("sync")
@PreAuthorize("hasRole('${Roles.NOMIS_SYNC}')")
class SyncController(
  private val tapApplication: SyncTapApplication,
  private val scheduledAbsence: SyncScheduledTemporaryAbsence,
  private val movement: SyncTapMovement,
) {
  @PutMapping("/temporary-absence-application/{personIdentifier}")
  fun syncTemporaryAbsenceApplication(
    @PathVariable personIdentifier: String,
    @RequestBody request: TapApplicationRequest,
  ): SyncResponse = tapApplication.sync(personIdentifier, request)

  @PutMapping("/scheduled-temporary-absence/{parentId}")
  fun syncScheduledTemporaryAbsence(
    @PathVariable parentId: UUID,
    @RequestBody request: ScheduledTemporaryAbsenceRequest,
  ): SyncResponse = scheduledAbsence.sync(parentId, request)

  @PutMapping("/temporary-absence-movement/{personIdentifier}")
  fun syncTemporaryAbsenceMovement(
    @PathVariable personIdentifier: String,
    @RequestBody request: TapMovementRequest,
  ): SyncResponse = movement.sync(personIdentifier, request)
}
