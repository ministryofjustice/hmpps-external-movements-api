package uk.gov.justice.digital.hmpps.externalmovementsapi.sync.read

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.location.Location
import uk.gov.justice.digital.hmpps.externalmovementsapi.sync.AtAndBy
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Schema(name = "SyncReadTapAuthorisation")
data class TapAuthorisation(
  val id: UUID,
  val personIdentifier: String,
  val prisonCode: String,
  val statusCode: String,
  val absenceTypeCode: String?,
  val absenceSubTypeCode: String?,
  val absenceReasonCode: String,
  val accompaniedByCode: String,
  val repeat: Boolean,
  val fromDate: LocalDate,
  val toDate: LocalDate,
  val created: AtAndBy,
  val updated: AtAndBy?,
  val notes: String?,
  val occurrences: List<Occurrence>,
) {
  @Schema(name = "SyncReadTapAuthorisationOccurrence")
  data class Occurrence(
    val id: UUID,
    val statusCode: String,
    val releaseAt: LocalDateTime,
    val returnBy: LocalDateTime,
    val location: Location,
    val absenceTypeCode: String?,
    val absenceSubTypeCode: String?,
    val absenceReasonCode: String,
    val accompaniedByCode: String,
    val transportCode: String,
    val notes: String?,
    val created: AtAndBy,
    val updated: AtAndBy?,
  )
}
