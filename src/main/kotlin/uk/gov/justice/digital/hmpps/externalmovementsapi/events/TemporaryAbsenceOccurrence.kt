package uk.gov.justice.digital.hmpps.externalmovementsapi.events

import uk.gov.justice.digital.hmpps.externalmovementsapi.context.DataSource
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.ExternalMovementContext
import java.util.UUID

data class TemporaryAbsenceInformation(
  override val id: UUID,
  override val source: DataSource,
) : AdditionalInformation,
  IdInformation

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

data class TemporaryAbsenceDenied(
  override val additionalInformation: TemporaryAbsenceInformation,
  override val personReference: PersonReference,
) : DomainEvent<TemporaryAbsenceInformation> {
  override val eventType: String = EVENT_TYPE
  override val description: String = "A temporary absence has been denied."

  companion object {
    const val EVENT_TYPE: String = "person.temporary-absence.denied"
    operator fun invoke(
      personIdentifier: String,
      id: UUID,
      dataSource: DataSource = ExternalMovementContext.get().source,
    ) = TemporaryAbsenceDenied(
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

data class TemporaryAbsenceAccompanimentChanged(
  override val additionalInformation: TemporaryAbsenceInformation,
  override val personReference: PersonReference,
) : DomainEvent<TemporaryAbsenceInformation> {
  override val eventType: String = EVENT_TYPE
  override val description: String = "The accompaniment of a temporary absence has been changed."

  companion object {
    const val EVENT_TYPE: String = "person.temporary-absence.accompaniment-changed"
    operator fun invoke(
      personIdentifier: String,
      id: UUID,
      dataSource: DataSource = ExternalMovementContext.get().source,
    ) = TemporaryAbsenceAccompanimentChanged(
      TemporaryAbsenceInformation(id, dataSource),
      PersonReference.withIdentifier(personIdentifier),
    )
  }
}

data class TemporaryAbsenceCommentsChanged(
  override val additionalInformation: TemporaryAbsenceInformation,
  override val personReference: PersonReference,
) : DomainEvent<TemporaryAbsenceInformation> {
  override val eventType: String = EVENT_TYPE
  override val description: String = "The comments on a temporary absence have been changed."

  companion object {
    const val EVENT_TYPE: String = "person.temporary-absence.comments-changed"
    operator fun invoke(
      personIdentifier: String,
      id: UUID,
      dataSource: DataSource = ExternalMovementContext.get().source,
    ) = TemporaryAbsenceCommentsChanged(
      TemporaryAbsenceInformation(id, dataSource),
      PersonReference.withIdentifier(personIdentifier),
    )
  }
}

data class TemporaryAbsenceTransportChanged(
  override val additionalInformation: TemporaryAbsenceInformation,
  override val personReference: PersonReference,
) : DomainEvent<TemporaryAbsenceInformation> {
  override val eventType: String = EVENT_TYPE
  override val description: String = "The transport for a temporary absence has been changed."

  companion object {
    const val EVENT_TYPE: String = "person.temporary-absence.transport-changed"
    operator fun invoke(
      personIdentifier: String,
      id: UUID,
      dataSource: DataSource = ExternalMovementContext.get().source,
    ) = TemporaryAbsenceTransportChanged(
      TemporaryAbsenceInformation(id, dataSource),
      PersonReference.withIdentifier(personIdentifier),
    )
  }
}

data class TemporaryAbsenceRecategorised(
  override val additionalInformation: TemporaryAbsenceInformation,
  override val personReference: PersonReference,
) : DomainEvent<TemporaryAbsenceInformation> {
  override val eventType: String = EVENT_TYPE
  override val description: String = "A temporary absence has been recategorised."

  companion object {
    const val EVENT_TYPE: String = "person.temporary-absence.recategorised"
    operator fun invoke(
      personIdentifier: String,
      id: UUID,
      dataSource: DataSource = ExternalMovementContext.get().source,
    ) = TemporaryAbsenceRecategorised(
      TemporaryAbsenceInformation(id, dataSource),
      PersonReference.withIdentifier(personIdentifier),
    )
  }
}

data class TemporaryAbsenceRelocated(
  override val additionalInformation: TemporaryAbsenceInformation,
  override val personReference: PersonReference,
) : DomainEvent<TemporaryAbsenceInformation> {
  override val eventType: String = EVENT_TYPE
  override val description: String = "A temporary absence has been relocated."

  companion object {
    const val EVENT_TYPE: String = "person.temporary-absence.relocated"
    operator fun invoke(
      personIdentifier: String,
      id: UUID,
      dataSource: DataSource = ExternalMovementContext.get().source,
    ) = TemporaryAbsenceRelocated(
      TemporaryAbsenceInformation(id, dataSource),
      PersonReference.withIdentifier(personIdentifier),
    )
  }
}

data class TemporaryAbsenceStarted(
  override val additionalInformation: TemporaryAbsenceInformation,
  override val personReference: PersonReference,
) : DomainEvent<TemporaryAbsenceInformation> {
  override val eventType: String = EVENT_TYPE
  override val description: String = "A temporary absence has started."

  companion object {
    const val EVENT_TYPE: String = "person.temporary-absence.started"
    operator fun invoke(
      personIdentifier: String,
      id: UUID,
      dataSource: DataSource = ExternalMovementContext.get().source,
    ) = TemporaryAbsenceStarted(
      TemporaryAbsenceInformation(id, dataSource),
      PersonReference.withIdentifier(personIdentifier),
    )
  }
}

data class TemporaryAbsenceCompleted(
  override val additionalInformation: TemporaryAbsenceInformation,
  override val personReference: PersonReference,
) : DomainEvent<TemporaryAbsenceInformation> {
  override val eventType: String = EVENT_TYPE
  override val description: String = "A temporary absence has completed."

  companion object {
    const val EVENT_TYPE: String = "person.temporary-absence.completed"
    operator fun invoke(
      personIdentifier: String,
      id: UUID,
      dataSource: DataSource = ExternalMovementContext.get().source,
    ) = TemporaryAbsenceCompleted(
      TemporaryAbsenceInformation(id, dataSource),
      PersonReference.withIdentifier(personIdentifier),
    )
  }
}
