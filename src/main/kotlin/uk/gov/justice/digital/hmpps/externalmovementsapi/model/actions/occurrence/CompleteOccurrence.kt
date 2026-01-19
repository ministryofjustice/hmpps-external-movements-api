package uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.occurrence

import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.occurrence.TemporaryAbsenceOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.DomainEvent
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.TemporaryAbsenceCompleted

data class CompleteOccurrence(
  override val reason: String? = null,
) : OccurrenceAction {
  override fun domainEvent(tao: TemporaryAbsenceOccurrence): DomainEvent<*> = TemporaryAbsenceCompleted(tao.person.identifier, tao.id)
}
