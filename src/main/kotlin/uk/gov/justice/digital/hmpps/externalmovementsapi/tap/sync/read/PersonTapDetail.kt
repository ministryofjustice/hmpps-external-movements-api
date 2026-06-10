package uk.gov.justice.digital.hmpps.externalmovementsapi.tap.sync.read

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.location.Location
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.domain.movement.TemporaryAbsenceMovement
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.domain.referencedata.AuthorisationStatus
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.domain.referencedata.OccurrenceStatus
import java.time.LocalDateTime
import java.util.UUID

data class PersonTapDetail(
  val scheduledAbsences: List<Authorisation>,
  val unscheduledMovements: List<Movement>,
) {
  @Schema(name = "ReconciliationAuthorisation")
  data class Authorisation(
    val id: UUID,
    val statusCode: AuthorisationStatus.Code,
    val prisonCode: String,
    val occurrences: List<Occurrence>,
  )

  @Schema(name = "ReconciliationOccurrence")
  data class Occurrence(
    val id: UUID,
    val statusCode: OccurrenceStatus.Code,
    val prisonCode: String,
    val reasonCode: String,
    val start: LocalDateTime,
    val end: LocalDateTime,
    val location: Location?,
    val movements: List<Movement>,
  )

  @Schema(name = "ReconciliationMovement")
  data class Movement(
    val id: UUID,
    val direction: TemporaryAbsenceMovement.Direction,
    val directionPrisonCode: String,
    val occurredAt: LocalDateTime,
    val absenceReasonCode: String,
    val accompaniedByCode: String,
    val location: Location,
    val comments: String?,
  )
}
