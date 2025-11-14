package uk.gov.justice.digital.hmpps.externalmovementsapi.model

import com.fasterxml.jackson.databind.JsonNode
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.location.Location
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.referencedata.CodedDescription
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class TapAuthorisation(
  val id: UUID,
  val person: Person,
  val status: CodedDescription,
  val absenceType: CodedDescription?,
  val absenceSubType: CodedDescription?,
  val absenceReasonCategory: CodedDescription?,
  val absenceReason: CodedDescription?,
  val accompaniedBy: CodedDescription,
  val repeat: Boolean,
  val fromDate: LocalDate,
  val toDate: LocalDate,
  val occurrences: List<Occurrence>,
  val schedule: JsonNode?,
  val notes: String?,
) {
  data class Occurrence(
    val id: UUID,
    val status: CodedDescription,
    val absenceType: CodedDescription?,
    val absenceSubType: CodedDescription?,
    val absenceReasonCategory: CodedDescription?,
    val absenceReason: CodedDescription?,
    val releaseAt: LocalDateTime,
    val returnBy: LocalDateTime,
    val location: Location,
    val accompaniedBy: CodedDescription,
    val transport: CodedDescription,
    val notes: String?,
  )
}
