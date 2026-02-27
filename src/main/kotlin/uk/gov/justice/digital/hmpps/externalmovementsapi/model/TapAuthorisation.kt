package uk.gov.justice.digital.hmpps.externalmovementsapi.model

import io.swagger.v3.oas.annotations.media.Schema
import tools.jackson.databind.JsonNode
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.prisonregister.Prison
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.location.Location
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.referencedata.CodedDescription
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.SequencedSet
import java.util.UUID

@Schema(name = "TapAuthorisation")
data class TapAuthorisation(
  val id: UUID,
  val prison: Prison,
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
  val totalOccurrenceCount: Long,
  val occurrences: List<Occurrence>,
  val locations: SequencedSet<Location>,
  val schedule: JsonNode?,
  val comments: String?,
) {
  @Schema(name = "TapAuthorisation.Occurrence")
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
  )
}
