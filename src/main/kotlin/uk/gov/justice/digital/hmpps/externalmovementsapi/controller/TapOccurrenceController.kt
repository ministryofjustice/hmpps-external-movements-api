package uk.gov.justice.digital.hmpps.externalmovementsapi.controller

import jakarta.validation.Valid
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.externalmovementsapi.access.Roles
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.TapOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.occurrence.OccurrenceAction
import uk.gov.justice.digital.hmpps.externalmovementsapi.service.GetTapOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.service.TapOccurrenceModifications
import java.util.UUID

@RestController
@RequestMapping("/temporary-absence-occurrences")
@PreAuthorize("hasRole('${Roles.EXTERNAL_MOVEMENTS_UI}')")
class TapOccurrenceController(
  private val get: GetTapOccurrence,
  private val modify: TapOccurrenceModifications,
) {
  @GetMapping("/{id}")
  fun getTapOccurrence(@PathVariable id: UUID): TapOccurrence = get.byId(id)

  @PutMapping("/{id}")
  fun applyActions(@PathVariable id: UUID, @Valid @RequestBody action: OccurrenceAction) {
    modify.apply(id, action)
  }
}
