package uk.gov.justice.digital.hmpps.externalmovementsapi.tap.model.actions.movement

import uk.gov.justice.digital.hmpps.externalmovementsapi.events.DomainEvent
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.TapMovementCommentsChanged
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.action.CommentsAction
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.domain.movement.TemporaryAbsenceMovement

data class ChangeMovementComments(
  override val comments: String?,
) : MovementAction,
  CommentsAction {
  override fun domainEvent(tam: TemporaryAbsenceMovement): DomainEvent<*> = TapMovementCommentsChanged(tam.person.identifier, tam.id)
}
