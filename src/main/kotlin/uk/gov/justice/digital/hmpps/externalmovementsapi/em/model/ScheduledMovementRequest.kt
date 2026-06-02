package uk.gov.justice.digital.hmpps.externalmovementsapi.em.model

import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode.NOT_REQUIRED
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.StartAndEnd
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.ValidStartAndEnd
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.SequencedSet

sealed interface ScheduledMovementsRequest : StartAndEnd<LocalDateTime> {
  @get:Schema(requiredMode = NOT_REQUIRED)
  val personIdentifiers: SequencedSet<String>

  @get:Schema(requiredMode = NOT_REQUIRED)
  override val start: LocalDateTime

  @get:Schema(requiredMode = NOT_REQUIRED)
  override val end: LocalDateTime
}

@ValidStartAndEnd
class SearchScheduledMovementsRequest(
  @Schema(requiredMode = NOT_REQUIRED)
  val movementTypes: Set<ScheduledMovementType> = setOf(),
  override val personIdentifiers: SequencedSet<String> = linkedSetOf(),
  start: LocalDateTime? = null,
  end: LocalDateTime? = null,
  @get:Schema(requiredMode = NOT_REQUIRED)
  val includeSensitive: Boolean = false,
  @get:Schema(requiredMode = NOT_REQUIRED)
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
