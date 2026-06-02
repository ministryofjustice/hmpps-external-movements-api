package uk.gov.justice.digital.hmpps.externalmovementsapi.tap.sync.read

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.location.Location
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.domain.movement.TemporaryAbsenceMovement
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.sync.AtAndBy
import java.time.LocalDateTime
import java.util.UUID

@Schema(name = "SyncReadTapMovement")
data class TapMovement(
  val id: UUID,
  val occurrenceId: UUID?,
  val personIdentifier: String,
  val occurredAt: LocalDateTime,
  val direction: TemporaryAbsenceMovement.Direction,
  val prisonCode: String,
  val absenceReasonCode: String,
  val location: Location,
  val accompaniedByCode: String,
  val accompaniedByComments: String?,
  val comments: String?,
  val created: AtAndBy,
  val updated: AtAndBy?,
)
