package uk.gov.justice.digital.hmpps.externalmovementsapi.tap.controller

import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.externalmovementsapi.access.Roles
import uk.gov.justice.digital.hmpps.externalmovementsapi.config.CaseloadIdHeader
import uk.gov.justice.digital.hmpps.externalmovementsapi.config.OpenApiTags.UI
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.AuditHistory
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.model.TapOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.model.actions.occurrence.OccurrenceAction
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.model.actions.occurrence.OccurrenceActions
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.service.GetTapOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.service.TapOccurrenceModifications
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.service.history.OccurrenceHistory
import java.util.UUID

@Tag(name = UI)
@RestController
@RequestMapping("/temporary-absence-occurrences")
@PreAuthorize("hasRole('${Roles.EXTERNAL_MOVEMENTS_UI}')")
class TapOccurrenceController(
  private val get: GetTapOccurrence,
  private val history: OccurrenceHistory,
  private val modify: TapOccurrenceModifications,
) {
  @GetMapping("/{id}")
  fun getTapOccurrence(@PathVariable id: UUID): TapOccurrence = get.byId(id)

  @GetMapping("/{id}/history")
  fun getTapOccurrenceHistory(@PathVariable id: UUID): AuditHistory = history.changes(id)

  @CaseloadIdHeader
  @PutMapping("/{id}")
  fun applyAction(@PathVariable id: UUID, @Valid @RequestBody action: OccurrenceAction) = modify.apply(id, action)

  @CaseloadIdHeader
  @PutMapping("/{id}/actions")
  fun applyActions(@PathVariable id: UUID, @Valid @RequestBody actions: OccurrenceActions) = modify.apply(id, actions)
}
