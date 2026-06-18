package uk.gov.justice.digital.hmpps.externalmovementsapi.tap.model.actions.movement

import uk.gov.justice.digital.hmpps.externalmovementsapi.events.DomainEvent
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.TapMovementRecategorised
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.domain.movement.TemporaryAbsenceMovement

data class ChangeMovementReason(
  val absenceReasonCode: String,
) : MovementAction {
  override fun domainEvent(tam: TemporaryAbsenceMovement): DomainEvent<*> = TapMovementRecategorised(tam.person.identifier, tam.id)
}
