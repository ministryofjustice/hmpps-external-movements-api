package uk.gov.justice.digital.hmpps.externalmovementsapi.events

import uk.gov.justice.digital.hmpps.externalmovementsapi.context.DataSource
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.ExternalMovementContext
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.model.integration.IntegrationUrlBuilder.movementUrl
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
  override val description: String = DESCRIPTION
  override val detailUrl: String = movementUrl(additionalInformation.id)

  companion object {
    const val EVENT_TYPE: String = "person.temporary-absence-movement.reversed"
    const val DESCRIPTION: String = "A temporary movement direction has been reversed."
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
  override val description: String = DESCRIPTION
  override val detailUrl: String = movementUrl(additionalInformation.id)

  companion object {
    const val EVENT_TYPE: String = "person.temporary-absence-movement.accompaniment-changed"
    const val DESCRIPTION: String = "The accompaniment of a temporary absence movement has been changed."
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
  override val description: String = DESCRIPTION
  override val detailUrl: String = movementUrl(additionalInformation.id)

  companion object {
    const val EVENT_TYPE: String = "person.temporary-absence-movement.comments-changed"
    const val DESCRIPTION: String = "The comments on a temporary absence movement have been changed."
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
  override val description: String = DESCRIPTION
  override val detailUrl: String = movementUrl(additionalInformation.id)

  companion object {
    const val EVENT_TYPE: String = "person.temporary-absence-movement.recategorised"
    const val DESCRIPTION: String = "A temporary absence movement has been recategorised."
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
  override val description: String = DESCRIPTION
  override val detailUrl: String = movementUrl(additionalInformation.id)

  companion object {
    const val EVENT_TYPE: String = "person.temporary-absence-movement.relocated"
    const val DESCRIPTION: String = "A temporary absence movement has been relocated."
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
  override val description: String = DESCRIPTION
  override val detailUrl: String = movementUrl(additionalInformation.id)

  companion object {
    const val EVENT_TYPE: String = "person.temporary-absence-movement.occurred-at-changed"
    const val DESCRIPTION: String = "When a temporary absence movement occurred has been changed."
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
  override val description: String = DESCRIPTION
  override val detailUrl: String = movementUrl(additionalInformation.id)

  companion object {
    const val EVENT_TYPE: String = "person.temporary-absence-movement.occurrence-changed"
    const val DESCRIPTION: String = "A temporary absence movement's occurrence has been changed."
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
