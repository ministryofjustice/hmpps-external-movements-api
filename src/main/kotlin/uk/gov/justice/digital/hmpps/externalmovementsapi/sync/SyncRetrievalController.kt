package uk.gov.justice.digital.hmpps.externalmovementsapi.sync

import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.externalmovementsapi.access.Roles
import uk.gov.justice.digital.hmpps.externalmovementsapi.sync.internal.AuthorisationRetriever
import uk.gov.justice.digital.hmpps.externalmovementsapi.sync.internal.MovementRetriever
import uk.gov.justice.digital.hmpps.externalmovementsapi.sync.internal.OccurrenceRetriever
import java.util.UUID

@RestController
@RequestMapping("sync")
@PreAuthorize("hasRole('${Roles.NOMIS_SYNC}')")
class SyncRetrievalController(
  private val authorisation: AuthorisationRetriever,
  private val occurrence: OccurrenceRetriever,
  private val movement: MovementRetriever,
) {
  @GetMapping("/temporary-absence-authorisations/{id}")
  fun findTapAuthorisationById(@PathVariable id: UUID) = authorisation.getById(id)

  @GetMapping("/temporary-absence-occurrences/{id}")
  fun findTapOccurrenceById(@PathVariable id: UUID) = occurrence.getById(id)

  @GetMapping("/temporary-absence-movements/{id}")
  fun findTapMovementById(@PathVariable id: UUID) = movement.getById(id)
}
