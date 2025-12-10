package uk.gov.justice.digital.hmpps.externalmovementsapi.model

import jakarta.validation.Valid
import jakarta.validation.constraints.FutureOrPresent
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.location.Location
import java.time.LocalDateTime

@ValidStartAndEnd
data class CreateOccurrenceRequest(
  @FutureOrPresent(message = "Absence cannot be scheduled in the past.")
  override val start: LocalDateTime,
  override val end: LocalDateTime,
  @Valid
  val location: Location,
  val comments: String?,
) : StartAndEnd<LocalDateTime>
