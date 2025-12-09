package uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.occurrence

import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.occurrence.TemporaryAbsenceOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.DomainEvent
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.TemporaryAbsenceTransportChanged

data class ChangeOccurrenceTransport(
  val transportCode: String,
  override val reason: String? = null,
) : OccurrenceAction {
  override fun domainEvent(tao: TemporaryAbsenceOccurrence): DomainEvent<*> = TemporaryAbsenceTransportChanged(tao.authorisation.person.identifier, tao.id)
}
