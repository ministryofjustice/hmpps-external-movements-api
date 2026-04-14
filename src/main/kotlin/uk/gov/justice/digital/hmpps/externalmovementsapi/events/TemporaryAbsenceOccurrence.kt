package uk.gov.justice.digital.hmpps.externalmovementsapi.events

import uk.gov.justice.digital.hmpps.externalmovementsapi.context.DataSource
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.ExternalMovementContext
import java.util.*

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
  override val description: String = DESCRIPTION

  companion object {
    const val EVENT_TYPE: String = "person.temporary-absence.scheduled"
    const val DESCRIPTION: String = "A temporary absence has been scheduled."
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

data class TemporaryAbsencePaused(
  override val additionalInformation: TemporaryAbsenceInformation,
  override val personReference: PersonReference,
) : DomainEvent<TemporaryAbsenceInformation> {
  override val eventType: String = EVENT_TYPE
  override val description: String = DESCRIPTION

  companion object {
    const val EVENT_TYPE: String = "person.temporary-absence.paused"
    const val DESCRIPTION: String = "A temporary absence has been paused."
    operator fun invoke(
      personIdentifier: String,
      id: UUID,
      dataSource: DataSource = ExternalMovementContext.get().source,
    ) = TemporaryAbsencePaused(
      TemporaryAbsenceInformation(id, dataSource),
      PersonReference.withIdentifier(personIdentifier),
    )
  }
}

data class TemporaryAbsenceResumed(
  override val additionalInformation: TemporaryAbsenceInformation,
  override val personReference: PersonReference,
) : DomainEvent<TemporaryAbsenceInformation> {
  override val eventType: String = EVENT_TYPE
  override val description: String = DESCRIPTION

  companion object {
    const val EVENT_TYPE: String = "person.temporary-absence.resumed"
    const val DESCRIPTION: String = "A temporary absence has been resumed."
    operator fun invoke(
      personIdentifier: String,
      id: UUID,
      dataSource: DataSource = ExternalMovementContext.get().source,
    ) = TemporaryAbsenceResumed(
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
  override val description: String = DESCRIPTION

  companion object {
    const val EVENT_TYPE: String = "person.temporary-absence.rescheduled"
    const val DESCRIPTION: String = "A temporary absence has been rescheduled."
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

data class TemporaryAbsenceUnscheduled(
  override val additionalInformation: TemporaryAbsenceInformation,
  override val personReference: PersonReference,
) : DomainEvent<TemporaryAbsenceInformation> {
  override val eventType: String = EVENT_TYPE
  override val description: String = DESCRIPTION

  companion object {
    const val EVENT_TYPE: String = "person.temporary-absence.unscheduled"
    const val DESCRIPTION: String = "A temporary absence has been unscheduled."
    operator fun invoke(
      personIdentifier: String,
      id: UUID,
      dataSource: DataSource = ExternalMovementContext.get().source,
    ) = TemporaryAbsenceUnscheduled(
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
  override val description: String = DESCRIPTION

  companion object {
    const val EVENT_TYPE: String = "person.temporary-absence.cancelled"
    const val DESCRIPTION: String = "A temporary absence has been cancelled."
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
  override val description: String = DESCRIPTION

  companion object {
    const val EVENT_TYPE: String = "person.temporary-absence.denied"
    const val DESCRIPTION: String = "A temporary absence has been denied."
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
  override val description: String = DESCRIPTION

  companion object {
    const val EVENT_TYPE: String = "person.temporary-absence.expired"
    const val DESCRIPTION: String = "A temporary absence has been expired."
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
  override val description: String = DESCRIPTION

  companion object {
    const val EVENT_TYPE: String = "person.temporary-absence.overdue"
    const val DESCRIPTION: String = "A temporary absence return is overdue"
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
  override val description: String = DESCRIPTION

  companion object {
    const val EVENT_TYPE: String = "person.temporary-absence.accompaniment-changed"
    const val DESCRIPTION: String = "The accompaniment of a temporary absence has been changed."
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
  override val description: String = DESCRIPTION

  companion object {
    const val EVENT_TYPE: String = "person.temporary-absence.comments-changed"
    const val DESCRIPTION: String = "The comments on a temporary absence have been changed."
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
  override val description: String = DESCRIPTION

  companion object {
    const val EVENT_TYPE: String = "person.temporary-absence.transport-changed"
    const val DESCRIPTION: String = "The transport for a temporary absence has been changed."
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
  override val description: String = DESCRIPTION

  companion object {
    const val EVENT_TYPE: String = "person.temporary-absence.recategorised"
    const val DESCRIPTION: String = "A temporary absence has been recategorised."
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
  override val description: String = DESCRIPTION

  companion object {
    const val EVENT_TYPE: String = "person.temporary-absence.relocated"
    const val DESCRIPTION: String = "A temporary absence has been relocated."
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
