package uk.gov.justice.digital.hmpps.externalmovementsapi.tap.model.integration

import uk.gov.justice.digital.hmpps.externalmovementsapi.model.location.Location
import java.time.LocalDateTime
import java.util.UUID

data class IntegrationOccurrence(
  val id: UUID,
  val authorisationId: UUID,
  val personIdentifier: String,
  val prisonCode: String,
  val status: IntegrationCodedDescription,
  val reason: IntegrationReason,
  val transport: IntegrationCodedDescription,
  val accompaniedBy: IntegrationCodedDescription,
  val start: LocalDateTime,
  val end: LocalDateTime,
  val location: Location,
  val comments: String?,
)
