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
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.AuditHistory
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.TapOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.occurrence.OccurrenceAction
import uk.gov.justice.digital.hmpps.externalmovementsapi.service.GetTapOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.service.TapOccurrenceModifications
import uk.gov.justice.digital.hmpps.externalmovementsapi.service.history.OccurrenceHistory
import java.util.UUID

@RestController
@RequestMapping("/temporary-absence-occurrences")
@PreAuthorize("hasRole('${Roles.TEMPORARY_ABSENCE_RW}')")
class TapOccurrenceController(
  private val get: GetTapOccurrence,
  private val history: OccurrenceHistory,
  private val modify: TapOccurrenceModifications,
) {
  @PreAuthorize("hasAnyRole('${Roles.TEMPORARY_ABSENCE_RO}', '${Roles.TEMPORARY_ABSENCE_RW}')")
  @GetMapping("/{id}")
  fun getTapOccurrence(@PathVariable id: UUID): TapOccurrence = get.byId(id)

  @PreAuthorize("hasAnyRole('${Roles.TEMPORARY_ABSENCE_RO}', '${Roles.TEMPORARY_ABSENCE_RW}')")
  @GetMapping("/{id}/history")
  fun getTapOccurrenceHistory(@PathVariable id: UUID): AuditHistory = history.changes(id)

  @PutMapping("/{id}")
  fun applyActions(@PathVariable id: UUID, @Valid @RequestBody action: OccurrenceAction) = modify.apply(id, action)
}
