package uk.gov.justice.digital.hmpps.externalmovementsapi.tap.model.actions.occurrence

import uk.gov.justice.digital.hmpps.externalmovementsapi.events.DomainEvent
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.TemporaryAbsenceResumed
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.domain.occurrence.TemporaryAbsenceOccurrence

data class ResumeOccurrence(
  override val reason: String? = null,
) : OccurrenceAction {
  override fun domainEvent(tao: TemporaryAbsenceOccurrence): DomainEvent<*> = TemporaryAbsenceResumed(tao.person.identifier, tao.id)
}
