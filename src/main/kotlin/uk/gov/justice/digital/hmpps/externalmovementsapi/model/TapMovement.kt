package uk.gov.justice.digital.hmpps.externalmovementsapi.model

import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.movement.TemporaryAbsenceMovement
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.location.Location
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.referencedata.CodedDescription
import java.time.LocalDateTime
import java.util.UUID

data class TapMovement(
  val id: UUID,
  val person: Person,
  val occurrence: Occurrence?,
  val occurredAt: LocalDateTime,
  val direction: TemporaryAbsenceMovement.Direction,
  val directionPrisonCode: String,
  val absenceReason: CodedDescription,
  val location: Location,
  val accompaniedBy: CodedDescription,
  val accompaniedByComments: String?,
  val comments: String?,
) {
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
