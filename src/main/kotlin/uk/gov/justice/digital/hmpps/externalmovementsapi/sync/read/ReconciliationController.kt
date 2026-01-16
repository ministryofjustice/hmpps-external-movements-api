package uk.gov.justice.digital.hmpps.externalmovementsapi.sync.read

import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.externalmovementsapi.access.Roles
import uk.gov.justice.digital.hmpps.externalmovementsapi.config.OpenApiTags
import uk.gov.justice.digital.hmpps.externalmovementsapi.sync.internal.GetPersonTemporaryAbsences

@Tag(name = OpenApiTags.SYNC)
@RestController
@PreAuthorize("hasRole('${Roles.NOMIS_SYNC}')")
class ReconciliationController(private val personTemporaryAbsences: GetPersonTemporaryAbsences) {
  @GetMapping("/reconciliation/{personIdentifier}/temporary-absences")
  fun countTemporaryAbsences(@PathVariable personIdentifier: String): PersonTapCounts = personTemporaryAbsences.count(personIdentifier)

  @GetMapping("/reconciliation-detail/{personIdentifier}/temporary-absences")
  fun getTemporaryAbsences(@PathVariable personIdentifier: String): PersonTapDetail = personTemporaryAbsences.detail(personIdentifier)
}
