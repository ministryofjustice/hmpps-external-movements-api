package uk.gov.justice.digital.hmpps.externalmovementsapi.events

import uk.gov.justice.digital.hmpps.externalmovementsapi.context.DataSource
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.ExternalMovementContext
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.integration.IntegrationUrlBuilder.movementUrl
import java.util.UUID

data class TapProgressInformation(
  override val id: UUID,
  val occurrenceId: UUID?,
  override val source: DataSource,
) : AdditionalInformation,
  IdInformation

data class TemporaryAbsenceStarted(
  override val additionalInformation: TapProgressInformation,
  override val personReference: PersonReference,
) : DomainEvent<TapProgressInformation> {
  override val eventType: String = EVENT_TYPE
  override val description: String = DESCRIPTION
  override val detailUrl: String = movementUrl(additionalInformation.id)

  companion object {
    const val EVENT_TYPE: String = "person.temporary-absence.started"
    const val DESCRIPTION: String = "A temporary absence has started."
    operator fun invoke(
      personIdentifier: String,
      id: UUID,
      occurrenceId: UUID?,
      dataSource: DataSource = ExternalMovementContext.get().source,
    ) = TemporaryAbsenceStarted(
      TapProgressInformation(id, occurrenceId, dataSource),
      PersonReference.withIdentifier(personIdentifier),
    )
  }
}

data class TemporaryAbsenceCompleted(
  override val additionalInformation: TapProgressInformation,
  override val personReference: PersonReference,
) : DomainEvent<TapProgressInformation> {
  override val eventType: String = EVENT_TYPE
  override val description: String = DESCRIPTION
  override val detailUrl: String = movementUrl(additionalInformation.id)

  companion object {
    const val EVENT_TYPE: String = "person.temporary-absence.completed"
    const val DESCRIPTION: String = "A temporary absence has completed."
    operator fun invoke(
      personIdentifier: String,
      id: UUID,
      occurrenceId: UUID?,
      dataSource: DataSource = ExternalMovementContext.get().source,
    ) = TemporaryAbsenceCompleted(
      TapProgressInformation(id, occurrenceId, dataSource),
      PersonReference.withIdentifier(personIdentifier),
    )
  }
}
