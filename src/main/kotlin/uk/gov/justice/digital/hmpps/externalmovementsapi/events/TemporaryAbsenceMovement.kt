package uk.gov.justice.digital.hmpps.externalmovementsapi.events

import uk.gov.justice.digital.hmpps.externalmovementsapi.context.DataSource
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.ExternalMovementContext
import java.util.UUID

data class TapMovementInformation(
  override val id: UUID,
  override val source: DataSource,
) : AdditionalInformation,
  IdInformation

data class TapMovementReversed(
  override val additionalInformation: TapMovementInformation,
  override val personReference: PersonReference,
) : DomainEvent<TapMovementInformation> {
  override val eventType: String = EVENT_TYPE
  override val description: String = "A temporary movement direction has been reversed."

  companion object {
    const val EVENT_TYPE: String = "person.temporary-absence-movement.reversed"
    operator fun invoke(
      personIdentifier: String,
      id: UUID,
      dataSource: DataSource = ExternalMovementContext.get().source,
    ) = TapMovementReversed(
      TapMovementInformation(id, dataSource),
      PersonReference.withIdentifier(personIdentifier),
    )
  }
}

data class TapMovementAccompanimentChanged(
  override val additionalInformation: TapMovementInformation,
  override val personReference: PersonReference,
) : DomainEvent<TapMovementInformation> {
  override val eventType: String = EVENT_TYPE
  override val description: String = "The accompaniment of a temporary absence movement has been changed."

  companion object {
    const val EVENT_TYPE: String = "person.temporary-absence-movement.accompaniment-changed"
    operator fun invoke(
      personIdentifier: String,
      id: UUID,
      dataSource: DataSource = ExternalMovementContext.get().source,
    ) = TapMovementAccompanimentChanged(
      TapMovementInformation(id, dataSource),
      PersonReference.withIdentifier(personIdentifier),
    )
  }
}

data class TapMovementCommentsChanged(
  override val additionalInformation: TapMovementInformation,
  override val personReference: PersonReference,
) : DomainEvent<TapMovementInformation> {
  override val eventType: String = EVENT_TYPE
  override val description: String = "The comments on a temporary absence movement have been changed."

  companion object {
    const val EVENT_TYPE: String = "person.temporary-absence-movement.comments-changed"
    operator fun invoke(
      personIdentifier: String,
      id: UUID,
      dataSource: DataSource = ExternalMovementContext.get().source,
    ) = TapMovementCommentsChanged(
      TapMovementInformation(id, dataSource),
      PersonReference.withIdentifier(personIdentifier),
    )
  }
}

data class TapMovementRecategorised(
  override val additionalInformation: TapMovementInformation,
  override val personReference: PersonReference,
) : DomainEvent<TapMovementInformation> {
  override val eventType: String = EVENT_TYPE
  override val description: String = "A temporary absence movement has been recategorised."

  companion object {
    const val EVENT_TYPE: String = "person.temporary-absence-movement.recategorised"
    operator fun invoke(
      personIdentifier: String,
      id: UUID,
      dataSource: DataSource = ExternalMovementContext.get().source,
    ) = TapMovementRecategorised(
      TapMovementInformation(id, dataSource),
      PersonReference.withIdentifier(personIdentifier),
    )
  }
}

data class TapMovementRelocated(
  override val additionalInformation: TapMovementInformation,
  override val personReference: PersonReference,
) : DomainEvent<TapMovementInformation> {
  override val eventType: String = EVENT_TYPE
  override val description: String = "A temporary absence movement has been relocated."

  companion object {
    const val EVENT_TYPE: String = "person.temporary-absence-movement.relocated"
    operator fun invoke(
      personIdentifier: String,
      id: UUID,
      dataSource: DataSource = ExternalMovementContext.get().source,
    ) = TapMovementRelocated(
      TapMovementInformation(id, dataSource),
      PersonReference.withIdentifier(personIdentifier),
    )
  }
}

data class TapMovementOccurredAtChanged(
  override val additionalInformation: TapMovementInformation,
  override val personReference: PersonReference,
) : DomainEvent<TapMovementInformation> {
  override val eventType: String = EVENT_TYPE
  override val description: String = "When a temporary absence movement occurred has been changed."

  companion object {
    const val EVENT_TYPE: String = "person.temporary-absence-movement.occurred-at-changed"
    operator fun invoke(
      personIdentifier: String,
      id: UUID,
      dataSource: DataSource = ExternalMovementContext.get().source,
    ) = TapMovementOccurredAtChanged(
      TapMovementInformation(id, dataSource),
      PersonReference.withIdentifier(personIdentifier),
    )
  }
}

data class TapMovementOccurrenceChanged(
  override val additionalInformation: TapMovementInformation,
  override val personReference: PersonReference,
) : DomainEvent<TapMovementInformation> {
  override val eventType: String = EVENT_TYPE
  override val description: String = "A temporary absence movement's occurrence has been changed."

  companion object {
    const val EVENT_TYPE: String = "person.temporary-absence-movement.occurrence-changed"
    operator fun invoke(
      personIdentifier: String,
      id: UUID,
      dataSource: DataSource = ExternalMovementContext.get().source,
    ) = TapMovementOccurrenceChanged(
      TapMovementInformation(id, dataSource),
      PersonReference.withIdentifier(personIdentifier),
    )
  }
}
