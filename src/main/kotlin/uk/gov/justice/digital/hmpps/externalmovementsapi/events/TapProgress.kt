package uk.gov.justice.digital.hmpps.externalmovementsapi.events

import uk.gov.justice.digital.hmpps.externalmovementsapi.context.DataSource
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.ExternalMovementContext
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
  override val description: String = "A temporary absence has started."

  companion object {
    const val EVENT_TYPE: String = "person.temporary-absence.started"
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
  override val description: String = "A temporary absence has completed."

  companion object {
    const val EVENT_TYPE: String = "person.temporary-absence.completed"
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
