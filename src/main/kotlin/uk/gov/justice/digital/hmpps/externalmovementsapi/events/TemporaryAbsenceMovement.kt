package uk.gov.justice.digital.hmpps.externalmovementsapi.events

import uk.gov.justice.digital.hmpps.externalmovementsapi.context.DataSource
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.ExternalMovementContext
import java.util.UUID

data class TapMovementInformation(
  override val id: UUID,
  override val source: DataSource,
) : AdditionalInformation,
  IdInformation

data class TapMovementOut(
  override val additionalInformation: TapMovementInformation,
  override val personReference: PersonReference,
) : DomainEvent<TapMovementInformation> {
  override val eventType: String = EVENT_TYPE
  override val description: String = "A person has been temporarily released from prison."

  companion object {
    const val EVENT_TYPE: String = "person.external-movement.recorded-out-temporarily"
    operator fun invoke(
      personIdentifier: String,
      id: UUID,
      dataSource: DataSource = ExternalMovementContext.get().source,
    ) = TapMovementOut(
      TapMovementInformation(id, dataSource),
      PersonReference.withIdentifier(personIdentifier),
    )
  }
}

data class TapMovementIn(
  override val additionalInformation: TapMovementInformation,
  override val personReference: PersonReference,
) : DomainEvent<TapMovementInformation> {
  override val eventType: String = EVENT_TYPE
  override val description: String = "A person has returned from a temporary release from prison."

  companion object {
    const val EVENT_TYPE: String = "person.external-movement.recorded-in-from-out-temporarily"
    operator fun invoke(
      personIdentifier: String,
      id: UUID,
      dataSource: DataSource = ExternalMovementContext.get().source,
    ) = TapMovementIn(
      TapMovementInformation(id, dataSource),
      PersonReference.withIdentifier(personIdentifier),
    )
  }
}

data class TapMovementReversed(
  override val additionalInformation: TapMovementInformation,
  override val personReference: PersonReference,
) : DomainEvent<TapMovementInformation> {
  override val eventType: String = EVENT_TYPE
  override val description: String = "A temporary movement direction has been reversed."

  companion object {
    const val EVENT_TYPE: String = "person.external-movement.reversed"
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
    const val EVENT_TYPE: String = "person.external-movement.accompaniment-changed"
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
    const val EVENT_TYPE: String = "person.external-movement.comments-changed"
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
    const val EVENT_TYPE: String = "person.external-movement.recategorised"
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
    const val EVENT_TYPE: String = "person.external-movement.relocated"
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
    const val EVENT_TYPE: String = "person.external-movement.occurred-at-changed"
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
