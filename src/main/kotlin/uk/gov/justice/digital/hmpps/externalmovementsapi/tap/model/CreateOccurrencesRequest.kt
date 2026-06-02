package uk.gov.justice.digital.hmpps.externalmovementsapi.tap.model

import jakarta.validation.Valid
import jakarta.validation.constraints.FutureOrPresent
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.StartAndEnd
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.ValidStartAndEnd
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.location.Location
import java.time.LocalDateTime

@ValidStartAndEnd
data class CreateOccurrencesRequest(
  @FutureOrPresent(message = "Absence cannot be scheduled in the past.")
  override val start: LocalDateTime? = null,
  override val end: LocalDateTime? = null,
  @Valid
  val location: Location? = null,
  val comments: String? = null,
  @Valid
  val occurrences: List<CreateOccurrenceRequest> = emptyList(),
) : StartAndEnd<LocalDateTime> {
  fun singleRequest(): CreateOccurrenceRequest {
    require(start != null && end != null && location != null) { "Start, end and location must not be null" }
    return CreateOccurrenceRequest(start, end, location, comments)
  }
}

@ValidStartAndEnd
data class CreateOccurrenceRequest(
  @FutureOrPresent(message = "Absence cannot be scheduled in the past.")
  override val start: LocalDateTime,
  override val end: LocalDateTime,
  @Valid
  val location: Location,
  val comments: String?,
) : StartAndEnd<LocalDateTime>
