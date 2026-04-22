package uk.gov.justice.digital.hmpps.externalmovementsapi.model

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.SequencedSet

interface ScheduledMovementsRequest : StartAndEnd<LocalDateTime> {
  val personIdentifiers: SequencedSet<String>
  override val start: LocalDateTime
  override val end: LocalDateTime
}

@ValidStartAndEnd
class SearchScheduledMovementsRequest(
  val movementTypes: Set<ScheduledMovementType> = setOf(),
  override val personIdentifiers: SequencedSet<String> = linkedSetOf(),
  start: LocalDateTime? = null,
  end: LocalDateTime? = null,
  val includeSensitive: Boolean = false,
  val includeLocation: Boolean = false,
) : ScheduledMovementsRequest {
  override val start: LocalDateTime = start ?: LocalDate.now().atStartOfDay()
  override val end: LocalDateTime = end ?: LocalDate.now().plusDays(1).atStartOfDay()
}

@ValidStartAndEnd
class SearchExternalActivitiesRequest(
  override val personIdentifiers: SequencedSet<String> = linkedSetOf(),
  start: LocalDateTime? = null,
  end: LocalDateTime? = null,
) : ScheduledMovementsRequest {
  override val start: LocalDateTime = start ?: LocalDate.now().atStartOfDay()
  override val end: LocalDateTime = end ?: LocalDate.now().plusDays(1).atStartOfDay()
}
