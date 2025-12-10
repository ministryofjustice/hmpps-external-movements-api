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
  val transport: CodedDescription,
  val repeat: Boolean,
  val start: LocalDate,
  val end: LocalDate,
  val occurrences: List<Occurrence>,
  val locations: List<Location>,
  val schedule: JsonNode?,
  val comments: String?,
) {
  val fromDate: LocalDate = start
  val toDate: LocalDate = end
  val notes: String? = comments

  data class Occurrence(
    val id: UUID,
    val status: CodedDescription,
    val absenceType: CodedDescription?,
    val absenceSubType: CodedDescription?,
    val absenceReasonCategory: CodedDescription?,
    val absenceReason: CodedDescription?,
    val start: LocalDateTime,
    val end: LocalDateTime,
    val location: Location,
    val accompaniedBy: CodedDescription,
    val transport: CodedDescription,
    val comments: String?,
  ) {
    val releaseAt: LocalDateTime = start
    val returnBy: LocalDateTime = end
    val notes: String? = comments
  }
}
