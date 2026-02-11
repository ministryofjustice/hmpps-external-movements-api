package uk.gov.justice.digital.hmpps.externalmovementsapi.sync.migrate

import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.externalmovementsapi.access.Roles
import uk.gov.justice.digital.hmpps.externalmovementsapi.config.OpenApiTags
import uk.gov.justice.digital.hmpps.externalmovementsapi.sync.internal.ResyncTapHierarchy

@Tag(name = OpenApiTags.SYNC)
@RestController
@RequestMapping("resync")
@PreAuthorize("hasRole('${Roles.NOMIS_SYNC}')")
class ResyncController(private val tap: ResyncTapHierarchy) {
  @PutMapping("/temporary-absences/{personIdentifier}")
  fun migrateTemporaryAbsences(
    @PathVariable personIdentifier: String,
    @RequestBody request: MigrateTapRequest,
  ): MigrateTapResponse = tap.resync(personIdentifier, request)
}
