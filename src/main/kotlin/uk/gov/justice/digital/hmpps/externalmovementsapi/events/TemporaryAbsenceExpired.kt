package uk.gov.justice.digital.hmpps.externalmovementsapi.events

import uk.gov.justice.digital.hmpps.externalmovementsapi.context.DataSource
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.ExternalMovementContext
import java.util.UUID

data class TemporaryAbsenceExpired(
  override val additionalInformation: TemporaryAbsenceExpiredInformation,
  override val personReference: PersonReference,
) : DomainEvent<TemporaryAbsenceExpiredInformation> {
  override val eventType: String = EVENT_TYPE
  override val description: String = "A temporary absence has expired."

  companion object {
    const val EVENT_TYPE: String = "person.temporary-absence.expired"
    operator fun invoke(
      personIdentifier: String,
      id: UUID,
      dataSource: DataSource = ExternalMovementContext.get().source,
    ) = TemporaryAbsenceExpired(
      TemporaryAbsenceExpiredInformation(id, dataSource),
      PersonReference.withIdentifier(personIdentifier),
    )
  }
}

data class TemporaryAbsenceExpiredInformation(
  val id: UUID,
  override val source: DataSource,
) : AdditionalInformation
