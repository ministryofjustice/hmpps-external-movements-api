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
data class SearchScheduledMovementsRequest(
  val movementTypes: Set<ScheduledMovementType> = setOf(),
  override val personIdentifiers: SequencedSet<String> = linkedSetOf(),
  override val start: LocalDateTime = LocalDate.now().atStartOfDay(),
  override val end: LocalDateTime = LocalDate.now().plusDays(1).atStartOfDay(),
  val includeSensitive: Boolean = false,
  val includeLocation: Boolean = false,
) : ScheduledMovementsRequest

@ValidStartAndEnd
data class SearchExternalActivitiesRequest(
  override val personIdentifiers: SequencedSet<String> = linkedSetOf(),
  override val start: LocalDateTime = LocalDate.now().atStartOfDay(),
  override val end: LocalDateTime = LocalDate.now().plusDays(1).atStartOfDay(),
) : ScheduledMovementsRequest
