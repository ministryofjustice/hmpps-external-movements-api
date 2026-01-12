package uk.gov.justice.digital.hmpps.externalmovementsapi.sync.write

import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.externalmovementsapi.access.Roles
import uk.gov.justice.digital.hmpps.externalmovementsapi.config.OpenApiTags
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.DataSource
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.ExternalMovementContext
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.set
import uk.gov.justice.digital.hmpps.externalmovementsapi.sync.internal.MoveTemporaryAbsences

@Tag(name = OpenApiTags.SYNC)
@RestController
@RequestMapping("move")
@PreAuthorize("hasRole('${Roles.NOMIS_SYNC}')")
class MoveController(private val temporaryAbsences: MoveTemporaryAbsences) {

  @PutMapping("/temporary-absences")
  fun moveTemporaryAbsences(@RequestBody request: MoveTemporaryAbsencesRequest) {
    ExternalMovementContext.get().copy(reason = "Prisoner booking moved", source = DataSource.NOMIS).set()
    temporaryAbsences.move(request)
  }
}
