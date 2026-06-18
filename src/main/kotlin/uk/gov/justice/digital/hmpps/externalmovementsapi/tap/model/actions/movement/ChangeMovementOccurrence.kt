package uk.gov.justice.digital.hmpps.externalmovementsapi.tap.model.actions.movement

import uk.gov.justice.digital.hmpps.externalmovementsapi.events.DomainEvent
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.TapMovementOccurrenceChanged
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.domain.movement.TemporaryAbsenceMovement
import java.util.UUID

data class ChangeMovementOccurrence(val occurrenceId: UUID?) : MovementAction {
  override fun domainEvent(tam: TemporaryAbsenceMovement): DomainEvent<*> = TapMovementOccurrenceChanged(tam.person.identifier, tam.id)
}
