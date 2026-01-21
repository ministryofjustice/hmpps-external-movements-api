package uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.movement

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

data class ChangeMovementOccurredAt(
  val occurredAt: LocalDateTime,
  override val reason: String? = null,
) : MovementAction {
  infix fun changes(occurredAt: LocalDateTime): Boolean = this.occurredAt.truncatedTo(ChronoUnit.SECONDS) == occurredAt.truncatedTo(ChronoUnit.SECONDS)
}
