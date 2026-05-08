package uk.gov.justice.digital.hmpps.externalmovementsapi.model.integration

import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.movement.TemporaryAbsenceMovement
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.location.Location
import java.time.LocalDateTime
import java.util.UUID

data class IntegrationMovement(
  val id: UUID,
  val occurrenceId: UUID?,
  val personIdentifier: String,
  val prisonCode: String,
  val direction: TemporaryAbsenceMovement.Direction,
  val reason: IntegrationReason,
  val accompaniedBy: IntegrationCodedDescription,
  val accompaniedByComments: String?,
  val occurredAt: LocalDateTime,
  val location: Location,
  val comments: String?,
)
