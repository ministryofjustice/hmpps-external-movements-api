package uk.gov.justice.digital.hmpps.externalmovementsapi.events

import uk.gov.justice.digital.hmpps.externalmovementsapi.context.DataSource
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.ExternalMovementContext
import java.util.UUID

data class TemporaryAbsenceAuthorisationMigrated(
  override val additionalInformation: TemporaryAbsenceAuthorisationInformation,
  override val personReference: PersonReference,
) : DomainEvent<TemporaryAbsenceAuthorisationInformation> {
  override val eventType: String = EVENT_TYPE
  override val description: String = "A temporary absence authorisation has been migrated."

  companion object {
    const val EVENT_TYPE: String = "person.temporary-absence-authorisation.migrated"

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
  override val description: String = "A temporary absence has been migrated."

  companion object {
    const val EVENT_TYPE: String = "person.temporary-absence.migrated"
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
  override val additionalInformation: ExternalMovementInformation,
  override val personReference: PersonReference,
) : DomainEvent<ExternalMovementInformation> {
  override val eventType: String = EVENT_TYPE
  override val description: String = "An external movement has been migrated."

  companion object {
    const val EVENT_TYPE: String = "person.external-movement.migrated"
    operator fun invoke(
      personIdentifier: String,
      id: UUID,
      dataSource: DataSource = ExternalMovementContext.get().source,
    ) = ExternalMovementMigrated(
      ExternalMovementInformation(id, dataSource),
      PersonReference.withIdentifier(personIdentifier),
    )
  }
}
