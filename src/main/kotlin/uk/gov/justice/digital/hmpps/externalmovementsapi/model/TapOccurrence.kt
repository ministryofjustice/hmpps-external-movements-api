package uk.gov.justice.digital.hmpps.externalmovementsapi.model

import com.fasterxml.jackson.databind.JsonNode
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.movement.TemporaryAbsenceMovement
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.location.Location
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.referencedata.CodedDescription
import java.time.LocalDateTime
import java.util.UUID

data class TapOccurrence(
  val id: UUID,
  val authorisation: Authorisation,
  val absenceType: CodedDescription?,
  val absenceSubType: CodedDescription?,
  val absenceReasonCategory: CodedDescription?,
  val absenceReason: CodedDescription?,
  val status: CodedDescription,
  val start: LocalDateTime,
  val end: LocalDateTime,
  val location: Location,
  val accompaniedBy: CodedDescription,
  val transport: CodedDescription,
  val contactInformation: String?,
  val scheduleReference: JsonNode?,
  val comments: String?,
  val occurrencePosition: Int,
  val totalOccurrences: Int,
  val movements: List<Movement>,
) {
  data class Authorisation(
    val id: UUID,
    val person: Person,
    val status: CodedDescription,
    val absenceType: CodedDescription?,
    val absenceSubType: CodedDescription?,
    val absenceReasonCategory: CodedDescription?,
    val absenceReason: CodedDescription?,
    val accompaniedBy: CodedDescription,
    val repeat: Boolean,
    val comments: String?,
  )
  data class Movement(val id: UUID, val occurredAt: LocalDateTime, val direction: TemporaryAbsenceMovement.Direction)
}
