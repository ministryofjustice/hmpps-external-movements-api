package uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.movement

import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.movement.TemporaryAbsenceMovement
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.DomainEvent
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.TapMovementReversed

data class ChangeMovementDirection(
  val direction: TemporaryAbsenceMovement.Direction,
  override val reason: String? = null,
) : MovementAction {
  override fun domainEvent(tam: TemporaryAbsenceMovement): DomainEvent<*> = TapMovementReversed(tam.person.identifier, tam.id, tam.occurrence?.id)
}
