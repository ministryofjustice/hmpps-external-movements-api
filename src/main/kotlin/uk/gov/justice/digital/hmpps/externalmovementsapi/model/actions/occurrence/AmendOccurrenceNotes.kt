package uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.occurrence

import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.occurrence.TemporaryAbsenceOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.DomainEvent
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.TemporaryAbsenceNotesChanged
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.NotesAction

data class AmendOccurrenceNotes(
  override val notes: String,
  override val reason: String? = null,
) : OccurrenceAction,
  NotesAction {
  override fun domainEvent(tao: TemporaryAbsenceOccurrence): DomainEvent<*> = TemporaryAbsenceNotesChanged(tao.authorisation.person.identifier, tao.id)
}
