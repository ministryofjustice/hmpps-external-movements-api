package uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.occurrence

import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.occurrence.TemporaryAbsenceOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.DomainEvent
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.TemporaryAbsenceScheduled

data class ScheduleOccurrence(override val reason: String? = null) : OccurrenceAction {
  override fun domainEvent(tao: TemporaryAbsenceOccurrence): DomainEvent<*> = TemporaryAbsenceScheduled(tao.authorisation.person.identifier, tao.id)
}
