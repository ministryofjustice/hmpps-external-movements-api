package uk.gov.justice.digital.hmpps.externalmovementsapi.events

import uk.gov.justice.digital.hmpps.externalmovementsapi.context.DataSource
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.ExternalMovementContext
import java.util.UUID

data class TemporaryAbsencePending(
  override val additionalInformation: TemporaryAbsencePendingInformation,
  override val personReference: PersonReference,
) : DomainEvent<TemporaryAbsencePendingInformation> {
  override val eventType: String = EVENT_TYPE
  override val description: String = "A temporary absence authorisation has been pending."

  companion object {
    const val EVENT_TYPE: String = "person.temporary-absence-authorisation.pending"

    operator fun invoke(
      personIdentifier: String,
      id: UUID,
      dataSource: DataSource = ExternalMovementContext.get().source,
    ) = TemporaryAbsencePending(
      TemporaryAbsencePendingInformation(id, dataSource),
      PersonReference.withIdentifier(personIdentifier),
    )
  }
}

data class TemporaryAbsencePendingInformation(
  val id: UUID,
  override val source: DataSource,
) : AdditionalInformation
