package uk.gov.justice.digital.hmpps.externalmovementsapi.events

import uk.gov.justice.digital.hmpps.externalmovementsapi.context.DataSource
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.ExternalMovementContext
import java.util.UUID

data class TemporaryAbsenceCancelled(
  override val additionalInformation: TemporaryAbsenceCancelledInformation,
  override val personReference: PersonReference,
) : DomainEvent<TemporaryAbsenceCancelledInformation> {
  override val eventType: String = EVENT_TYPE
  override val description: String = "A temporary absence has been cancelled."

  companion object {
    const val EVENT_TYPE: String = "person.temporary-absence.cancelled"
    operator fun invoke(
      personIdentifier: String,
      id: UUID,
      dataSource: DataSource = ExternalMovementContext.get().source,
    ) = TemporaryAbsenceCancelled(
      TemporaryAbsenceCancelledInformation(id, dataSource),
      PersonReference.withIdentifier(personIdentifier),
    )
  }
}

data class TemporaryAbsenceCancelledInformation(
  val id: UUID,
  override val source: DataSource,
) : AdditionalInformation
