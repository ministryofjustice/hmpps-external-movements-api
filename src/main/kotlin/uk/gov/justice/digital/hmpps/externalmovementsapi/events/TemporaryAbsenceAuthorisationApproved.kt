package uk.gov.justice.digital.hmpps.externalmovementsapi.events

import uk.gov.justice.digital.hmpps.externalmovementsapi.context.DataSource
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.ExternalMovementContext
import java.util.UUID

data class TemporaryAbsenceAuthorised(
  override val additionalInformation: TemporaryAbsenceAuthorisedInformation,
  override val personReference: PersonReference,
) : DomainEvent<TemporaryAbsenceAuthorisedInformation> {
  override val eventType: String = EVENT_TYPE
  override val description: String = "A temporary absence authorisation has been approved."

  companion object {
    const val EVENT_TYPE: String = "person.temporary-absence-authorisation.approved"
  }
}

data class TemporaryAbsenceAuthorisedInformation(
  val id: UUID,
  override val source: DataSource = ExternalMovementContext.get().source,
) : AdditionalInformation
