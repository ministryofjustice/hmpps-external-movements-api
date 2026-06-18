package uk.gov.justice.digital.hmpps.externalmovementsapi.tap.model.actions.movement

import uk.gov.justice.digital.hmpps.externalmovementsapi.events.DomainEvent
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.TapMovementOccurredAtChanged
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.domain.movement.TemporaryAbsenceMovement
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

data class ChangeMovementOccurredAt(
  val occurredAt: LocalDateTime,
) : MovementAction {
  infix fun changes(occurredAt: LocalDateTime): Boolean = !this.occurredAt.truncatedTo(ChronoUnit.SECONDS).isEqual(occurredAt.truncatedTo(ChronoUnit.SECONDS))

  override fun domainEvent(tam: TemporaryAbsenceMovement): DomainEvent<*> = TapMovementOccurredAtChanged(tam.person.identifier, tam.id)
}
