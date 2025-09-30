package uk.gov.justice.digital.hmpps.externalmovementsapi.sync

import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.externalmovementsapi.access.Roles
import java.util.UUID

@RestController
@RequestMapping("sync")
@PreAuthorize("hasRole('${Roles.NOMIS_SYNC}')")
class SyncController(
  private val tapApplication: SyncTapApplication,
  private val scheduledAbsence: SyncScheduledTemporaryAbsence,
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
}
