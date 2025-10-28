package uk.gov.justice.digital.hmpps.externalmovementsapi.sync

import uk.gov.justice.digital.hmpps.externalmovementsapi.model.location.Location
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class TapAuthorisation(
  val id: UUID,
  val personIdentifier: String,
  val prisonCode: String,
  val statusCode: String,
  val absenceTypeCode: String?,
  val absenceSubTypeCode: String?,
  val absenceReasonCode: String,
  val repeat: Boolean,
  val fromDate: LocalDate,
  val toDate: LocalDate,
  val submittedAt: LocalDateTime,
  val notes: String?,
  val occurrences: List<Occurrence>,
) {
  data class Occurrence(
    val id: UUID,
    val statusCode: String,
    val releaseAt: LocalDateTime,
    val returnBy: LocalDateTime,
    val location: Location,
    val accompaniedByCode: String,
    val transportCode: String,
    val notes: String?,
  )
}
