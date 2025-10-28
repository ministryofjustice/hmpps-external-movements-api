package uk.gov.justice.digital.hmpps.externalmovementsapi.sync

import uk.gov.justice.digital.hmpps.externalmovementsapi.model.location.Location
import java.time.LocalDateTime
import java.util.UUID

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
  data class Authorisation(
    val id: UUID,
    val statusCode: String,
    val absenceTypeCode: String?,
    val absenceSubTypeCode: String?,
    val absenceReasonCode: String,
    val repeat: Boolean,
    val submittedAt: LocalDateTime,
  )
}
