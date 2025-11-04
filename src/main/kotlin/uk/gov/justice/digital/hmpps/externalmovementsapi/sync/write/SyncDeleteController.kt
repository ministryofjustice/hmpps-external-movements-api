package uk.gov.justice.digital.hmpps.externalmovementsapi.sync.write

import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.externalmovementsapi.access.Roles
import uk.gov.justice.digital.hmpps.externalmovementsapi.config.OpenApiTags
import uk.gov.justice.digital.hmpps.externalmovementsapi.sync.internal.SyncTapAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.sync.internal.SyncTapMovement
import uk.gov.justice.digital.hmpps.externalmovementsapi.sync.internal.SyncTapOccurrence
import java.util.UUID

@Tag(name = OpenApiTags.SYNC)
@RestController
@RequestMapping("sync")
@PreAuthorize("hasRole('${Roles.NOMIS_SYNC}')")
class SyncDeleteController(
  private val authorisation: SyncTapAuthorisation,
  private val occurrence: SyncTapOccurrence,
  private val movement: SyncTapMovement,
) {
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @DeleteMapping("/temporary-absence-authorisations/{id}")
  fun deleteTapAuthorisationById(@PathVariable id: UUID) = authorisation.deleteById(id)

  @ResponseStatus(HttpStatus.NO_CONTENT)
  @DeleteMapping("/temporary-absence-occurrences/{id}")
  fun deleteTapOccurrenceById(@PathVariable id: UUID) = occurrence.deleteById(id)

  @ResponseStatus(HttpStatus.NO_CONTENT)
  @DeleteMapping("/temporary-absence-movements/{id}")
  fun deleteTapMovementById(@PathVariable id: UUID) = movement.deleteById(id)
}
