package uk.gov.justice.digital.hmpps.externalmovementsapi.events

import uk.gov.justice.digital.hmpps.externalmovementsapi.context.DataSource
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.ExternalMovementContext
import java.util.UUID

data class TemporaryAbsenceAuthorisationInformation(
  val id: UUID,
  override val source: DataSource,
) : AdditionalInformation

data class TemporaryAbsenceAuthorisationPending(
  override val additionalInformation: TemporaryAbsenceAuthorisationInformation,
  override val personReference: PersonReference,
) : DomainEvent<TemporaryAbsenceAuthorisationInformation> {
  override val eventType: String = EVENT_TYPE
  override val description: String = "A temporary absence authorisation has been pending."

  companion object {
    const val EVENT_TYPE: String = "person.temporary-absence-authorisation.pending"

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
  override val description: String = "A temporary absence authorisation has been approved."

  companion object {
    const val EVENT_TYPE: String = "person.temporary-absence-authorisation.approved"

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

data class TemporaryAbsenceAuthorisationDenied(
  override val additionalInformation: TemporaryAbsenceAuthorisationInformation,
  override val personReference: PersonReference,
) : DomainEvent<TemporaryAbsenceAuthorisationInformation> {
  override val eventType: String = EVENT_TYPE
  override val description: String = "A temporary absence authorisation has been denied."

  companion object {
    const val EVENT_TYPE: String = "person.temporary-absence-authorisation.denied"

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
  override val description: String = "A temporary absence authorisation has been cancelled."

  companion object {
    const val EVENT_TYPE: String = "person.temporary-absence-authorisation.cancelled"

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

data class TemporaryAbsenceAuthorisationExpired(
  override val additionalInformation: TemporaryAbsenceAuthorisationInformation,
  override val personReference: PersonReference,
) : DomainEvent<TemporaryAbsenceAuthorisationInformation> {
  override val eventType: String = EVENT_TYPE
  override val description: String = "A temporary absence authorisation has expired."

  companion object {
    const val EVENT_TYPE: String = "person.temporary-absence-authorisation.expired"

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
  override val description: String = "The accompaniment of a temporary absence authorisation has been changed."

  companion object {
    const val EVENT_TYPE: String = "person.temporary-absence-authorisation.accompaniment-changed"

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
  override val description: String = "The comments on a temporary absence authorisation have been changed."

  companion object {
    const val EVENT_TYPE: String = "person.temporary-absence-authorisation.comments-changed"

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
  override val description: String = "The date range of an absence authorisation has changed."

  companion object {
    const val EVENT_TYPE: String = "person.temporary-absence-authorisation.date-range-changed"

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
  override val description: String = "The transport for a temporary absence authorisation has been changed."

  companion object {
    const val EVENT_TYPE: String = "person.temporary-absence-authorisation.transport-changed"

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
  override val description: String = "An temporary absence authorisation has been recategorised."

  companion object {
    const val EVENT_TYPE: String = "person.temporary-absence-authorisation.recategorised"

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
