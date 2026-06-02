package uk.gov.justice.digital.hmpps.externalmovementsapi.tap.model.actions.occurrence

import uk.gov.justice.digital.hmpps.externalmovementsapi.events.DomainEvent
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.TemporaryAbsenceTransportChanged
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.domain.occurrence.TemporaryAbsenceOccurrence

data class ChangeOccurrenceTransport(
  val transportCode: String,
  override val reason: String? = null,
) : OccurrenceAction {
  override fun domainEvent(tao: TemporaryAbsenceOccurrence): DomainEvent<*> = TemporaryAbsenceTransportChanged(tao.person.identifier, tao.id)
}
