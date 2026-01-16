package uk.gov.justice.digital.hmpps.externalmovementsapi.sync.read

import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.movement.TemporaryAbsenceMovement
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.AuthorisationStatus
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.OccurrenceStatus
import java.util.UUID

data class PersonTapDetail(
  val scheduledAbsences: List<Authorisation>,
  val unscheduledMovements: List<Movement>,
) {
  data class Authorisation(val id: UUID, val statusCode: AuthorisationStatus.Code, val occurrences: List<Occurrence>)
  data class Occurrence(val id: UUID, val statusCode: OccurrenceStatus.Code, val movements: List<Movement>)
  data class Movement(val id: UUID, val direction: TemporaryAbsenceMovement.Direction)
}
