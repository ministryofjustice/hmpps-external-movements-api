package uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.movement

import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.movement.TemporaryAbsenceMovement
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.DomainEvent
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.TapMovementAccompanimentChanged
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.CommentsAction

data class ChangeMovementAccompaniment(
  val accompaniedByCode: String,
  override val comments: String?,
  override val reason: String? = null,
) : MovementAction,
  CommentsAction {
  override fun domainEvent(tam: TemporaryAbsenceMovement): DomainEvent<*> = TapMovementAccompanimentChanged(tam.person.identifier, tam.id, tam.occurrence?.id)
}
