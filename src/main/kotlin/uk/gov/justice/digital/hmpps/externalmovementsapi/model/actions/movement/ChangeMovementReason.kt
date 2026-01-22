package uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.movement

import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.movement.TemporaryAbsenceMovement
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.DomainEvent
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.TapMovementRecategorised

data class ChangeMovementReason(
  val absenceReasonCode: String,
  override val reason: String? = null,
) : MovementAction {
  override fun domainEvent(tam: TemporaryAbsenceMovement): DomainEvent<*> = TapMovementRecategorised(tam.person.identifier, tam.id)
}
