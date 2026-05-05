package uk.gov.justice.digital.hmpps.externalmovementsapi.model.integration

import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceData
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.location.Location
import java.time.LocalDate
import java.util.UUID

data class IntegrationAuthorisation(
  val id: UUID,
  val personIdentifier: String,
  val prisonCode: String,
  val status: IntegrationCodedDescription,
  val reason: IntegrationReason,
  val transport: IntegrationCodedDescription,
  val accompaniedBy: IntegrationCodedDescription,
  val repeat: Boolean,
  val start: LocalDate,
  val end: LocalDate,
  val locations: List<Location>,
  val comments: String?,
)

data class IntegrationCodedDescription(val code: String, val description: String)
data class IntegrationReason(val code: String, val description: String, val fullDescription: String?)

fun ReferenceData.forIntegration() = IntegrationCodedDescription(code, description)
