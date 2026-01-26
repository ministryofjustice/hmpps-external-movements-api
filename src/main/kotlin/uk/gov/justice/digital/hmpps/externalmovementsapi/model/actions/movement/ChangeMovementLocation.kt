package uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.movement

import jakarta.validation.Valid
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.movement.TemporaryAbsenceMovement
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.DomainEvent
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.TapMovementRelocated
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.location.Location

data class ChangeMovementLocation(
  @Valid val location: Location,
  override val reason: String? = null,
) : MovementAction {
  override fun domainEvent(tam: TemporaryAbsenceMovement): DomainEvent<*> = TapMovementRelocated(tam.person.identifier, tam.id, tam.occurrence?.id)
}
