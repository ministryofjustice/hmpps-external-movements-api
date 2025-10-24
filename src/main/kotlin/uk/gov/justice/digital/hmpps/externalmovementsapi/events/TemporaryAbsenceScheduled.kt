package uk.gov.justice.digital.hmpps.externalmovementsapi.events

import uk.gov.justice.digital.hmpps.externalmovementsapi.context.DataSource
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.ExternalMovementContext
import java.util.UUID

data class TemporaryAbsenceScheduled(
  override val additionalInformation: TemporaryAbsenceScheduledInformation,
  override val personReference: PersonReference,
) : DomainEvent<TemporaryAbsenceScheduledInformation> {
  override val eventType: String = EVENT_TYPE
  override val description: String = "A temporary absence has been scheduled."

  companion object {
    const val EVENT_TYPE: String = "person.temporary-absence.scheduled"
  }
}

data class TemporaryAbsenceScheduledInformation(
  val id: UUID,
  override val source: DataSource = ExternalMovementContext.get().source,
) : AdditionalInformation
