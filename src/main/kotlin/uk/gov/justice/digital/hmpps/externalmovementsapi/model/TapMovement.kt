package uk.gov.justice.digital.hmpps.externalmovementsapi.model

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.movement.TemporaryAbsenceMovement
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.prisonregister.Prison
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.location.Location
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.referencedata.CodedDescription
import java.time.LocalDateTime
import java.util.UUID

@Schema(name = "TapMovement")
data class TapMovement(
  val id: UUID,
  val person: Person,
  val occurrence: Occurrence?,
  val occurredAt: LocalDateTime,
  val direction: TemporaryAbsenceMovement.Direction,
  val prison: Prison,
  val absenceReason: CodedDescription,
  val location: Location,
  val accompaniedBy: CodedDescription,
  val accompaniedByComments: String?,
  val comments: String?,
) {
  @Schema(name = "TapMovement.Occurrence")
  data class Occurrence(
    val id: UUID,
    val absenceType: CodedDescription?,
    val absenceSubType: CodedDescription?,
    val absenceReasonCategory: CodedDescription?,
    val absenceReason: CodedDescription?,
    val status: CodedDescription,
    val start: LocalDateTime,
    val end: LocalDateTime,
  )
}
