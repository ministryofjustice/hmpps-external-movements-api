package uk.gov.justice.digital.hmpps.externalmovementsapi.events

import uk.gov.justice.digital.hmpps.externalmovementsapi.context.DataSource
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.ExternalMovementContext
import java.util.UUID

data class TemporaryAbsenceInformation(
  val id: UUID,
  override val source: DataSource,
) : AdditionalInformation

data class TemporaryAbsenceScheduled(
  override val additionalInformation: TemporaryAbsenceInformation,
  override val personReference: PersonReference,
) : DomainEvent<TemporaryAbsenceInformation> {
  override val eventType: String = EVENT_TYPE
  override val description: String = "A temporary absence has been scheduled."

  companion object {
    const val EVENT_TYPE: String = "person.temporary-absence.scheduled"
    operator fun invoke(
      personIdentifier: String,
      id: UUID,
      dataSource: DataSource = ExternalMovementContext.get().source,
    ) = TemporaryAbsenceScheduled(
      TemporaryAbsenceInformation(id, dataSource),
      PersonReference.withIdentifier(personIdentifier),
    )
  }
}

data class TemporaryAbsenceRescheduled(
  override val additionalInformation: TemporaryAbsenceInformation,
  override val personReference: PersonReference,
) : DomainEvent<TemporaryAbsenceInformation> {
  override val eventType: String = EVENT_TYPE
  override val description: String = "A temporary absence has been rescheduled."

  companion object {
    const val EVENT_TYPE: String = "person.temporary-absence.rescheduled"
    operator fun invoke(
      personIdentifier: String,
      id: UUID,
      dataSource: DataSource = ExternalMovementContext.get().source,
    ) = TemporaryAbsenceRescheduled(
      TemporaryAbsenceInformation(id, dataSource),
      PersonReference.withIdentifier(personIdentifier),
    )
  }
}

data class TemporaryAbsenceCancelled(
  override val additionalInformation: TemporaryAbsenceInformation,
  override val personReference: PersonReference,
) : DomainEvent<TemporaryAbsenceInformation> {
  override val eventType: String = EVENT_TYPE
  override val description: String = "A temporary absence has been cancelled."

  companion object {
    const val EVENT_TYPE: String = "person.temporary-absence.cancelled"
    operator fun invoke(
      personIdentifier: String,
      id: UUID,
      dataSource: DataSource = ExternalMovementContext.get().source,
    ) = TemporaryAbsenceCancelled(
      TemporaryAbsenceInformation(id, dataSource),
      PersonReference.withIdentifier(personIdentifier),
    )
  }
}

data class TemporaryAbsenceExpired(
  override val additionalInformation: TemporaryAbsenceInformation,
  override val personReference: PersonReference,
) : DomainEvent<TemporaryAbsenceInformation> {
  override val eventType: String = EVENT_TYPE
  override val description: String = "A temporary absence has expired."

  companion object {
    const val EVENT_TYPE: String = "person.temporary-absence.expired"
    operator fun invoke(
      personIdentifier: String,
      id: UUID,
      dataSource: DataSource = ExternalMovementContext.get().source,
    ) = TemporaryAbsenceExpired(
      TemporaryAbsenceInformation(id, dataSource),
      PersonReference.withIdentifier(personIdentifier),
    )
  }
}

data class TemporaryAbsenceOverdue(
  override val additionalInformation: TemporaryAbsenceInformation,
  override val personReference: PersonReference,
) : DomainEvent<TemporaryAbsenceInformation> {
  override val eventType: String = EVENT_TYPE
  override val description: String = "A temporary absence return is overdue."

  companion object {
    const val EVENT_TYPE: String = "person.temporary-absence.overdue"
    operator fun invoke(
      personIdentifier: String,
      id: UUID,
      dataSource: DataSource = ExternalMovementContext.get().source,
    ) = TemporaryAbsenceOverdue(
      TemporaryAbsenceInformation(id, dataSource),
      PersonReference.withIdentifier(personIdentifier),
    )
  }
}
