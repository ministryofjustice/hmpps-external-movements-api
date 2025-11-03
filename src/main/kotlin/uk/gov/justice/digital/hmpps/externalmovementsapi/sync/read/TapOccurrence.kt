package uk.gov.justice.digital.hmpps.externalmovementsapi.sync.read

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.location.Location
import uk.gov.justice.digital.hmpps.externalmovementsapi.sync.AtAndBy
import java.time.LocalDateTime
import java.util.UUID

@Schema(name = "SyncReadTapOccurrence")
data class TapOccurrence(
  val id: UUID,
  val authorisation: Authorisation,
  val statusCode: String,
  val releaseAt: LocalDateTime,
  val returnBy: LocalDateTime,
  val location: Location,
  val accompaniedByCode: String,
  val transportCode: String,
  val notes: String?,
) {
  @Schema(name = "SyncReadTapOccurrenceAuthorisation")
  data class Authorisation(
    val id: UUID,
    val statusCode: String,
    val absenceTypeCode: String?,
    val absenceSubTypeCode: String?,
    val absenceReasonCode: String,
    val repeat: Boolean,
    val submitted: AtAndBy,
  )
}
