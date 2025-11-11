package uk.gov.justice.digital.hmpps.externalmovementsapi.events

import uk.gov.justice.digital.hmpps.externalmovementsapi.context.DataSource
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.ExternalMovementContext
import java.util.UUID

data class TemporaryAbsenceOverdue(
  override val additionalInformation: TemporaryAbsenceOverdueInformation,
  override val personReference: PersonReference,
) : DomainEvent<TemporaryAbsenceOverdueInformation> {
  override val eventType: String = EVENT_TYPE
  override val description: String = "A temporary absence return is overdue."

  companion object {
    const val EVENT_TYPE: String = "person.temporary-absence.overdue"
    operator fun invoke(
      personIdentifier: String,
      id: UUID,
      dataSource: DataSource = ExternalMovementContext.get().source,
    ) = TemporaryAbsenceOverdue(
      TemporaryAbsenceOverdueInformation(id, dataSource),
      PersonReference.withIdentifier(personIdentifier),
    )
  }
}

data class TemporaryAbsenceOverdueInformation(
  val id: UUID,
  override val source: DataSource,
) : AdditionalInformation
