package uk.gov.justice.digital.hmpps.externalmovementsapi.sync.read

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.movement.TemporaryAbsenceMovement
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.location.Location
import uk.gov.justice.digital.hmpps.externalmovementsapi.sync.AtAndBy
import uk.gov.justice.digital.hmpps.externalmovementsapi.sync.write.TapMovement.AtAndByWithPrison
import java.time.LocalDateTime
import java.util.UUID

@Schema(name = "SyncReadTapMovement")
data class TapMovement(
  val id: UUID,
  val occurrenceId: UUID?,
  val personIdentifier: String,
  val occurredAt: LocalDateTime,
  val direction: TemporaryAbsenceMovement.Direction,
  val absenceReasonCode: String,
  val location: Location,
  val accompaniedByCode: String,
  val accompaniedByNotes: String?,
  val notes: String?,
  val created: AtAndByWithPrison,
  val updated: AtAndBy?,
)
