package uk.gov.justice.digital.hmpps.externalmovementsapi.model

import io.swagger.v3.oas.annotations.media.Schema
import tools.jackson.databind.JsonNode
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.movement.TemporaryAbsenceMovement
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.prisonregister.Prison
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.location.Location
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.referencedata.CodedDescription
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Schema(name = "TapOccurrence")
data class TapOccurrence(
  val id: UUID,
  val prison: Prison,
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
  @Schema(name = "TapOccurrence.Authorisation")
  data class Authorisation(
    val id: UUID,
    val person: Person,
    val status: CodedDescription,
    val start: LocalDate,
    val end: LocalDate,
    val absenceType: CodedDescription?,
    val absenceSubType: CodedDescription?,
    val absenceReasonCategory: CodedDescription?,
    val absenceReason: CodedDescription?,
    val accompaniedBy: CodedDescription,
    val repeat: Boolean,
    val comments: String?,
  )

  @Schema(name = "TapOccurrence.Movement")
  data class Movement(
    val id: UUID,
    val occurredAt: LocalDateTime,
    val direction: TemporaryAbsenceMovement.Direction,
    val location: Location,
  )
}
