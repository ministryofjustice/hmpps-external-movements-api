package uk.gov.justice.digital.hmpps.externalmovementsapi.events

import uk.gov.justice.digital.hmpps.externalmovementsapi.context.DataSource
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.ExternalMovementContext
import java.util.UUID

data class TemporaryAbsenceRescheduled(
  override val additionalInformation: TemporaryAbsenceRescheduledInformation,
  override val personReference: PersonReference,
) : DomainEvent<TemporaryAbsenceRescheduledInformation> {
  override val eventType: String = EVENT_TYPE
  override val description: String = "A temporary absence has been rescheduled."

  companion object {
    const val EVENT_TYPE: String = "person.temporary-absence.rescheduled"
    operator fun invoke(
      personIdentifier: String,
      id: UUID,
      dataSource: DataSource = ExternalMovementContext.get().source,
    ) = TemporaryAbsenceRescheduled(
      TemporaryAbsenceRescheduledInformation(id, dataSource),
      PersonReference.withIdentifier(personIdentifier),
    )
  }
}

data class TemporaryAbsenceRescheduledInformation(
  val id: UUID,
  override val source: DataSource,
) : AdditionalInformation
