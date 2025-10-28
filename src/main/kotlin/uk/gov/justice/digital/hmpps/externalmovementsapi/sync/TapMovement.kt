package uk.gov.justice.digital.hmpps.externalmovementsapi.sync

import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.TemporaryAbsenceMovement
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.location.Location
import java.time.LocalDateTime
import java.util.UUID

data class TapMovement(
  val id: UUID,
  val occurrenceId: UUID?,
  val occurredAt: LocalDateTime,
  val direction: TemporaryAbsenceMovement.Direction,
  val absenceReasonCode: String,
  val location: Location,
  val accompaniedByCode: String,
  val accompaniedByNotes: String?,
  val notes: String?,
  val recordedByPrisonCode: String,
)
