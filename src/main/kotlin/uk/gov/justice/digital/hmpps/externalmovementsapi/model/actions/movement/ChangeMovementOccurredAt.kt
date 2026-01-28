package uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.movement

import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.movement.TemporaryAbsenceMovement
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.DomainEvent
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.TapMovementOccurredAtChanged
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

data class ChangeMovementOccurredAt(
  val occurredAt: LocalDateTime,
  override val reason: String? = null,
) : MovementAction {
  infix fun changes(occurredAt: LocalDateTime): Boolean = !this.occurredAt.truncatedTo(ChronoUnit.SECONDS).isEqual(occurredAt.truncatedTo(ChronoUnit.SECONDS))

  override fun domainEvent(tam: TemporaryAbsenceMovement): DomainEvent<*> = TapMovementOccurredAtChanged(tam.person.identifier, tam.id)
}
