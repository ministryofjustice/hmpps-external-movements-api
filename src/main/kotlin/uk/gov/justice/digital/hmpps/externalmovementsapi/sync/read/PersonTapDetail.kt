package uk.gov.justice.digital.hmpps.externalmovementsapi.sync.read

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.movement.TemporaryAbsenceMovement
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.AuthorisationStatus
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.OccurrenceStatus
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.location.Location
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
  data class Movement(val id: UUID, val direction: TemporaryAbsenceMovement.Direction, val directionPrisonCode: String)
}
