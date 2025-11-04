package uk.gov.justice.digital.hmpps.externalmovementsapi.sync.write

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.externalmovementsapi.access.Roles
import uk.gov.justice.digital.hmpps.externalmovementsapi.config.OpenApiTags
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.TemporaryAbsenceMovement
import uk.gov.justice.digital.hmpps.externalmovementsapi.sync.AtAndBy
import uk.gov.justice.digital.hmpps.externalmovementsapi.sync.internal.SyncTapAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.sync.internal.SyncTapMovement
import uk.gov.justice.digital.hmpps.externalmovementsapi.sync.internal.SyncTapOccurrence
import java.util.UUID

@Tag(name = OpenApiTags.SYNC)
@RestController
@RequestMapping("sync")
@PreAuthorize("hasRole('${Roles.NOMIS_SYNC}')")
class SyncController(
  private val authorisation: SyncTapAuthorisation,
  private val occurrence: SyncTapOccurrence,
  private val movement: SyncTapMovement,
) {
  @Operation(deprecated = true)
  @PutMapping("/temporary-absence-application/{personIdentifier}")
  fun syncTemporaryAbsenceApplication(
    @PathVariable personIdentifier: String,
    @RequestBody request: TapApplicationRequest,
  ): SyncResponse = authorisation.sync(personIdentifier, request.toNew())

  @Operation(deprecated = true)
  @PutMapping("/scheduled-temporary-absence/{parentId}")
  fun syncScheduledTemporaryAbsence(
    @PathVariable parentId: UUID,
    @RequestBody request: ScheduledTemporaryAbsenceRequest,
  ): SyncResponse = occurrence.sync(parentId, request.toNew())

  @Operation(deprecated = true)
  @PutMapping("/temporary-absence-movement/{personIdentifier}")
  fun syncTapMovement(
    @PathVariable personIdentifier: String,
    @RequestBody request: TapMovementRequest,
  ): SyncResponse = movement.sync(personIdentifier, request.toNew())

  @Operation(hidden = true)
  @PutMapping("/temporary-absence-authorisations/{personIdentifier}")
  fun syncTemporaryAbsenceAuthorisation(
    @PathVariable personIdentifier: String,
    @RequestBody request: TapAuthorisation,
  ): SyncResponse = authorisation.sync(personIdentifier, request)

  @Operation(hidden = true)
  @PutMapping("/temporary-absence-authorisations/{authorisationId}/occurrences")
  fun syncTemporaryAbsenceOccurrence(
    @PathVariable authorisationId: UUID,
    @RequestBody request: TapOccurrence,
  ): SyncResponse = occurrence.sync(authorisationId, request)

  @Operation(hidden = true)
  @PutMapping("/temporary-absence-movements/{personIdentifier}")
  fun syncTemporaryAbsenceMovement(
    @PathVariable personIdentifier: String,
    @RequestBody request: TapMovement,
  ): SyncResponse = movement.sync(personIdentifier, request)
}

private fun TapApplicationRequest.toNew() = TapAuthorisation(
  id,
  requireNotNull(prisonId),
  tapAuthStatusCode.name,
  temporaryAbsenceType,
  temporaryAbsenceSubType,
  eventSubType,
  isRepeating(),
  fromDate,
  toDate,
  comment,
  AtAndBy(audit.createDatetime, audit.createUsername),
  approvedAt?.let { AtAndBy(it, requireNotNull(approvedBy)) },
  movementApplicationId,
)

private fun ScheduledTemporaryAbsenceRequest.toNew() = TapOccurrence(
  id,
  eventStatus,
  startTime,
  returnTime,
  location.asLocation(),
  escortOrDefault(),
  transportTypeOrDefault(),
  comment,
  AtAndBy(audit.createDatetime, audit.createUsername),
  if (cancelled) AtAndBy(cancelledAt!!, cancelledBy!!) else null,
  eventId,
)

private fun TapMovementRequest.toNew() = TapMovement(
  id,
  occurrenceId,
  movementDateTime,
  when (direction) {
    TapMovementRequest.Direction.IN -> TemporaryAbsenceMovement.Direction.IN
    TapMovementRequest.Direction.OUT -> TemporaryAbsenceMovement.Direction.OUT
  },
  movementReason,
  location.asLocation(),
  escortOrDefault(),
  escortText,
  commentText,
  TapMovement.AtAndByWithPrison(audit.createDatetime, audit.createUsername, prisonCodeOrDefault()),
  legacyId,
)
