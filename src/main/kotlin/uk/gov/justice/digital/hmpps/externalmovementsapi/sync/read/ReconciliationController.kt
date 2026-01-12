package uk.gov.justice.digital.hmpps.externalmovementsapi.sync.read

import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.externalmovementsapi.access.Roles
import uk.gov.justice.digital.hmpps.externalmovementsapi.config.OpenApiTags
import uk.gov.justice.digital.hmpps.externalmovementsapi.sync.internal.CountPersonTemporaryAbsences

@Tag(name = OpenApiTags.SYNC)
@RestController
@RequestMapping("reconciliation/{personIdentifier}")
@PreAuthorize("hasRole('${Roles.NOMIS_SYNC}')")
class ReconciliationController(private val count: CountPersonTemporaryAbsences) {
  @GetMapping("/temporary-absences")
  fun getTemporaryAbsences(@PathVariable personIdentifier: String): PersonTapCounts = count.temporaryAbsences(personIdentifier)
}
