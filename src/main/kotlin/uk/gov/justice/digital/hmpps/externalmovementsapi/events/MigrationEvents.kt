package uk.gov.justice.digital.hmpps.externalmovementsapi.events

import uk.gov.justice.digital.hmpps.externalmovementsapi.context.DataSource
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.ExternalMovementContext
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.model.integration.IntegrationUrlBuilder.authorisationUrl
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.model.integration.IntegrationUrlBuilder.movementUrl
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.model.integration.IntegrationUrlBuilder.occurrenceUrl
import java.util.UUID

data class TemporaryAbsenceAuthorisationMigrated(
  override val additionalInformation: TemporaryAbsenceAuthorisationInformation,
  override val personReference: PersonReference,
) : DomainEvent<TemporaryAbsenceAuthorisationInformation> {
  override val eventType: String = EVENT_TYPE
  override val description: String = DESCRIPTION
  override val detailUrl: String = authorisationUrl(additionalInformation.id)

  companion object {
    const val EVENT_TYPE: String = "person.temporary-absence-authorisation.migrated"
    const val DESCRIPTION: String = "A temporary absence authorisation has been migrated."

    operator fun invoke(
      personIdentifier: String,
      id: UUID,
      dataSource: DataSource = ExternalMovementContext.get().source,
    ) = TemporaryAbsenceAuthorisationMigrated(
      TemporaryAbsenceAuthorisationInformation(id, dataSource),
      PersonReference.withIdentifier(personIdentifier),
    )
  }
}

data class TemporaryAbsenceMigrated(
  override val additionalInformation: TemporaryAbsenceInformation,
  override val personReference: PersonReference,
) : DomainEvent<TemporaryAbsenceInformation> {
  override val eventType: String = EVENT_TYPE
  override val description: String = DESCRIPTION
  override val detailUrl: String = occurrenceUrl(additionalInformation.id)

  companion object {
    const val EVENT_TYPE: String = "person.temporary-absence.migrated"
    const val DESCRIPTION: String = "A temporary absence has been migrated."
    operator fun invoke(
      personIdentifier: String,
      id: UUID,
      dataSource: DataSource = ExternalMovementContext.get().source,
    ) = TemporaryAbsenceMigrated(
      TemporaryAbsenceInformation(id, dataSource),
      PersonReference.withIdentifier(personIdentifier),
    )
  }
}

data class ExternalMovementMigrated(
  override val additionalInformation: TapMovementInformation,
  override val personReference: PersonReference,
) : DomainEvent<TapMovementInformation> {
  override val eventType: String = EVENT_TYPE
  override val description: String = DESCRIPTION
  override val detailUrl: String = movementUrl(additionalInformation.id)

  companion object {
    const val EVENT_TYPE: String = "person.external-movement.migrated"
    const val DESCRIPTION: String = "An external movement has been migrated."
    operator fun invoke(
      personIdentifier: String,
      id: UUID,
      dataSource: DataSource = ExternalMovementContext.get().source,
    ) = ExternalMovementMigrated(
      TapMovementInformation(id, dataSource),
      PersonReference.withIdentifier(personIdentifier),
    )
  }
}
