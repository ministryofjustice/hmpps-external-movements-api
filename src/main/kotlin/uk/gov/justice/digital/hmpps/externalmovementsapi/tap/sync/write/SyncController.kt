package uk.gov.justice.digital.hmpps.externalmovementsapi.tap.sync.write

import com.microsoft.applicationinsights.TelemetryClient
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.externalmovementsapi.access.Roles
import uk.gov.justice.digital.hmpps.externalmovementsapi.config.OpenApiTags
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.sync.internal.SyncTapAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.sync.internal.SyncTapMovement
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.sync.internal.SyncTapOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.sync.internal.response
import java.util.UUID

@Tag(name = OpenApiTags.SYNC)
@RestController
@RequestMapping("sync")
@PreAuthorize("hasRole('${Roles.NOMIS_SYNC}')")
class SyncController(
  private val authorisation: SyncTapAuthorisation,
  private val occurrence: SyncTapOccurrence,
  private val movement: SyncTapMovement,
  private val telemetryClient: TelemetryClient,
) {
  @ResponseStatus(HttpStatus.ACCEPTED)
  @PutMapping("/temporary-absence-authorisations/{personIdentifier}")
  fun syncTemporaryAbsenceAuthorisation(
    @PathVariable personIdentifier: String,
    @RequestBody request: TapAuthorisation,
  ): SyncResponse {
    val (res, status) = authorisation.sync(request)
    status.response(telemetryClient)
    return res
  }

  @ResponseStatus(HttpStatus.ACCEPTED)
  @PutMapping("/temporary-absence-authorisations/{authorisationId}/occurrences")
  fun syncTemporaryAbsenceOccurrence(
    @PathVariable authorisationId: UUID,
    @RequestBody request: TapOccurrence,
  ): SyncResponse {
    val (res, status) = occurrence.sync(request)
    status.response(telemetryClient)
    return res
  }

  @PutMapping("/temporary-absence-movements/{personIdentifier}")
  fun syncTemporaryAbsenceMovement(
    @PathVariable personIdentifier: String,
    @RequestBody request: TapMovement,
  ): SyncResponse = movement.sync(personIdentifier, request)
}
