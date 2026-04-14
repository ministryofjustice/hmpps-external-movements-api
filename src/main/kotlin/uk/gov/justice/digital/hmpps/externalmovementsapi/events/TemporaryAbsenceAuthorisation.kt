package uk.gov.justice.digital.hmpps.externalmovementsapi.events

import uk.gov.justice.digital.hmpps.externalmovementsapi.context.DataSource
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.ExternalMovementContext
import java.util.UUID

data class TemporaryAbsenceAuthorisationInformation(
  override val id: UUID,
  override val source: DataSource,
) : AdditionalInformation,
  IdInformation

data class TemporaryAbsenceAuthorisationPending(
  override val additionalInformation: TemporaryAbsenceAuthorisationInformation,
  override val personReference: PersonReference,
) : DomainEvent<TemporaryAbsenceAuthorisationInformation> {
  override val eventType: String = EVENT_TYPE
  override val description: String = DESCRIPTION

  companion object {
    const val EVENT_TYPE: String = "person.temporary-absence-authorisation.pending"
    const val DESCRIPTION: String = "A temporary absence authorisation is pending."
    operator fun invoke(
      personIdentifier: String,
      id: UUID,
      dataSource: DataSource = ExternalMovementContext.get().source,
    ) = TemporaryAbsenceAuthorisationPending(
      TemporaryAbsenceAuthorisationInformation(id, dataSource),
      PersonReference.withIdentifier(personIdentifier),
    )
  }
}

data class TemporaryAbsenceAuthorisationApproved(
  override val additionalInformation: TemporaryAbsenceAuthorisationInformation,
  override val personReference: PersonReference,
) : DomainEvent<TemporaryAbsenceAuthorisationInformation> {
  override val eventType: String = EVENT_TYPE
  override val description: String = DESCRIPTION

  companion object {
    const val EVENT_TYPE: String = "person.temporary-absence-authorisation.approved"
    const val DESCRIPTION: String = "A temporary absence authorisation has been approved."
    operator fun invoke(
      personIdentifier: String,
      id: UUID,
      dataSource: DataSource = ExternalMovementContext.get().source,
    ) = TemporaryAbsenceAuthorisationApproved(
      TemporaryAbsenceAuthorisationInformation(id, dataSource),
      PersonReference.withIdentifier(personIdentifier),
    )
  }
}

data class TemporaryAbsenceAuthorisationPaused(
  override val additionalInformation: TemporaryAbsenceAuthorisationInformation,
  override val personReference: PersonReference,
) : DomainEvent<TemporaryAbsenceAuthorisationInformation> {
  override val eventType: String = EVENT_TYPE
  override val description: String = DESCRIPTION

  companion object {
    const val EVENT_TYPE: String = "person.temporary-absence-authorisation.paused"
    const val DESCRIPTION: String = "A temporary absence authorisation has been paused."
    operator fun invoke(
      personIdentifier: String,
      id: UUID,
      dataSource: DataSource = ExternalMovementContext.get().source,
    ) = TemporaryAbsenceAuthorisationPaused(
      TemporaryAbsenceAuthorisationInformation(id, dataSource),
      PersonReference.withIdentifier(personIdentifier),
    )
  }
}

data class TemporaryAbsenceAuthorisationResumed(
  override val additionalInformation: TemporaryAbsenceAuthorisationInformation,
  override val personReference: PersonReference,
) : DomainEvent<TemporaryAbsenceAuthorisationInformation> {
  override val eventType: String = EVENT_TYPE
  override val description: String = DESCRIPTION

  companion object {
    const val EVENT_TYPE: String = "person.temporary-absence-authorisation.resumed"
    const val DESCRIPTION: String = "A temporary absence authorisation has been resumed."
    operator fun invoke(
      personIdentifier: String,
      id: UUID,
      dataSource: DataSource = ExternalMovementContext.get().source,
    ) = TemporaryAbsenceAuthorisationResumed(
      TemporaryAbsenceAuthorisationInformation(id, dataSource),
      PersonReference.withIdentifier(personIdentifier),
    )
  }
}

data class TemporaryAbsenceAuthorisationDenied(
  override val additionalInformation: TemporaryAbsenceAuthorisationInformation,
  override val personReference: PersonReference,
) : DomainEvent<TemporaryAbsenceAuthorisationInformation> {
  override val eventType: String = EVENT_TYPE
  override val description: String = DESCRIPTION

  companion object {
    const val EVENT_TYPE: String = "person.temporary-absence-authorisation.denied"
    const val DESCRIPTION: String = "A temporary absence authorisation has been denied."
    operator fun invoke(
      personIdentifier: String,
      id: UUID,
      dataSource: DataSource = ExternalMovementContext.get().source,
    ) = TemporaryAbsenceAuthorisationDenied(
      TemporaryAbsenceAuthorisationInformation(id, dataSource),
      PersonReference.withIdentifier(personIdentifier),
    )
  }
}

data class TemporaryAbsenceAuthorisationCancelled(
  override val additionalInformation: TemporaryAbsenceAuthorisationInformation,
  override val personReference: PersonReference,
) : DomainEvent<TemporaryAbsenceAuthorisationInformation> {
  override val eventType: String = EVENT_TYPE
  override val description: String = DESCRIPTION

  companion object {
    const val EVENT_TYPE: String = "person.temporary-absence-authorisation.cancelled"
    const val DESCRIPTION: String = "A temporary absence authorisation has been cancelled."
    operator fun invoke(
      personIdentifier: String,
      id: UUID,
      dataSource: DataSource = ExternalMovementContext.get().source,
    ) = TemporaryAbsenceAuthorisationCancelled(
      TemporaryAbsenceAuthorisationInformation(id, dataSource),
      PersonReference.withIdentifier(personIdentifier),
    )
  }
}

data class TemporaryAbsenceAuthorisationDeferred(
  override val additionalInformation: TemporaryAbsenceAuthorisationInformation,
  override val personReference: PersonReference,
) : DomainEvent<TemporaryAbsenceAuthorisationInformation> {
  override val eventType: String = EVENT_TYPE
  override val description: String = DESCRIPTION

  companion object {
    const val EVENT_TYPE: String = "person.temporary-absence-authorisation.deferred"
    const val DESCRIPTION: String = "A temporary absence authorisation has been deferred."
    operator fun invoke(
      personIdentifier: String,
      id: UUID,
      dataSource: DataSource = ExternalMovementContext.get().source,
    ) = TemporaryAbsenceAuthorisationDeferred(
      TemporaryAbsenceAuthorisationInformation(id, dataSource),
      PersonReference.withIdentifier(personIdentifier),
    )
  }
}

data class TemporaryAbsenceAuthorisationExpired(
  override val additionalInformation: TemporaryAbsenceAuthorisationInformation,
  override val personReference: PersonReference,
) : DomainEvent<TemporaryAbsenceAuthorisationInformation> {
  override val eventType: String = EVENT_TYPE
  override val description: String = DESCRIPTION

  companion object {
    const val EVENT_TYPE: String = "person.temporary-absence-authorisation.expired"
    const val DESCRIPTION: String = "A temporary absence authorisation has expired."
    operator fun invoke(
      personIdentifier: String,
      id: UUID,
      dataSource: DataSource = ExternalMovementContext.get().source,
    ) = TemporaryAbsenceAuthorisationExpired(
      TemporaryAbsenceAuthorisationInformation(id, dataSource),
      PersonReference.withIdentifier(personIdentifier),
    )
  }
}

data class TemporaryAbsenceAuthorisationAccompanimentChanged(
  override val additionalInformation: TemporaryAbsenceAuthorisationInformation,
  override val personReference: PersonReference,
) : DomainEvent<TemporaryAbsenceAuthorisationInformation> {
  override val eventType: String = EVENT_TYPE
  override val description: String = DESCRIPTION

  companion object {
    const val EVENT_TYPE: String = "person.temporary-absence-authorisation.accompaniment-changed"
    const val DESCRIPTION: String = "The accompaniment of a temporary absence authorisation has been changed."
    operator fun invoke(
      personIdentifier: String,
      id: UUID,
      dataSource: DataSource = ExternalMovementContext.get().source,
    ) = TemporaryAbsenceAuthorisationAccompanimentChanged(
      TemporaryAbsenceAuthorisationInformation(id, dataSource),
      PersonReference.withIdentifier(personIdentifier),
    )
  }
}

data class TemporaryAbsenceAuthorisationCommentsChanged(
  override val additionalInformation: TemporaryAbsenceAuthorisationInformation,
  override val personReference: PersonReference,
) : DomainEvent<TemporaryAbsenceAuthorisationInformation> {
  override val eventType: String = EVENT_TYPE
  override val description: String = DESCRIPTION

  companion object {
    const val EVENT_TYPE: String = "person.temporary-absence-authorisation.comments-changed"
    const val DESCRIPTION: String = "The comments on a temporary absence authorisation have been changed."
    operator fun invoke(
      personIdentifier: String,
      id: UUID,
      dataSource: DataSource = ExternalMovementContext.get().source,
    ) = TemporaryAbsenceAuthorisationCommentsChanged(
      TemporaryAbsenceAuthorisationInformation(id, dataSource),
      PersonReference.withIdentifier(personIdentifier),
    )
  }
}

data class TemporaryAbsenceAuthorisationDateRangeChanged(
  override val additionalInformation: TemporaryAbsenceAuthorisationInformation,
  override val personReference: PersonReference,
) : DomainEvent<TemporaryAbsenceAuthorisationInformation> {
  override val eventType: String = EVENT_TYPE
  override val description: String = DESCRIPTION

  companion object {
    const val EVENT_TYPE: String = "person.temporary-absence-authorisation.date-range-changed"
    const val DESCRIPTION: String = "The date range of a temporary absence authorisation has been changed."
    operator fun invoke(
      personIdentifier: String,
      id: UUID,
      dataSource: DataSource = ExternalMovementContext.get().source,
    ) = TemporaryAbsenceAuthorisationDateRangeChanged(
      TemporaryAbsenceAuthorisationInformation(id, dataSource),
      PersonReference.withIdentifier(personIdentifier),
    )
  }
}

data class TemporaryAbsenceAuthorisationTransportChanged(
  override val additionalInformation: TemporaryAbsenceAuthorisationInformation,
  override val personReference: PersonReference,
) : DomainEvent<TemporaryAbsenceAuthorisationInformation> {
  override val eventType: String = EVENT_TYPE
  override val description: String = DESCRIPTION

  companion object {
    const val EVENT_TYPE: String = "person.temporary-absence-authorisation.transport-changed"
    const val DESCRIPTION: String = "The transport for a temporary absence authorisation has been changed."
    operator fun invoke(
      personIdentifier: String,
      id: UUID,
      dataSource: DataSource = ExternalMovementContext.get().source,
    ) = TemporaryAbsenceAuthorisationTransportChanged(
      TemporaryAbsenceAuthorisationInformation(id, dataSource),
      PersonReference.withIdentifier(personIdentifier),
    )
  }
}

data class TemporaryAbsenceAuthorisationRecategorised(
  override val additionalInformation: TemporaryAbsenceAuthorisationInformation,
  override val personReference: PersonReference,
) : DomainEvent<TemporaryAbsenceAuthorisationInformation> {
  override val eventType: String = EVENT_TYPE
  override val description: String = DESCRIPTION

  companion object {
    const val EVENT_TYPE: String = "person.temporary-absence-authorisation.recategorised"
    const val DESCRIPTION: String = "A temporary absence authorisation has been recategorised."
    operator fun invoke(
      personIdentifier: String,
      id: UUID,
      dataSource: DataSource = ExternalMovementContext.get().source,
    ) = TemporaryAbsenceAuthorisationRecategorised(
      TemporaryAbsenceAuthorisationInformation(id, dataSource),
      PersonReference.withIdentifier(personIdentifier),
    )
  }
}

data class TemporaryAbsenceAuthorisationRelocated(
  override val additionalInformation: TemporaryAbsenceAuthorisationInformation,
  override val personReference: PersonReference,
) : DomainEvent<TemporaryAbsenceAuthorisationInformation> {
  override val eventType: String = EVENT_TYPE
  override val description: String = DESCRIPTION

  companion object {
    const val EVENT_TYPE: String = "person.temporary-absence-authorisation.relocated"
    const val DESCRIPTION: String = "The list of locations related to this authorisation has changed."
    operator fun invoke(
      personIdentifier: String,
      id: UUID,
      dataSource: DataSource = ExternalMovementContext.get().source,
    ) = TemporaryAbsenceAuthorisationRelocated(
      TemporaryAbsenceAuthorisationInformation(id, dataSource),
      PersonReference.withIdentifier(personIdentifier),
    )
  }
}
