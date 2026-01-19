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
  val transportCode: String,
  val repeat: Boolean,
  val start: LocalDate,
  val end: LocalDate,
  val created: AtAndBy,
  val updated: AtAndBy?,
  val comments: String?,
  val occurrences: List<Occurrence>,
) {
  @Schema(name = "SyncReadTapAuthorisationOccurrence")
  data class Occurrence(
    val id: UUID,
    val statusCode: String,
    val start: LocalDateTime,
    val end: LocalDateTime,
    val location: Location,
    val absenceTypeCode: String?,
    val absenceSubTypeCode: String?,
    val absenceReasonCode: String,
    val accompaniedByCode: String,
    val transportCode: String,
    val comments: String?,
    val created: AtAndBy,
    val updated: AtAndBy?,
  )
}
