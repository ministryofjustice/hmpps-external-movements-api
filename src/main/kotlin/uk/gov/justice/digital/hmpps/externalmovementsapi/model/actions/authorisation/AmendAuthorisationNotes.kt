package uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.authorisation

import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.authorisation.TemporaryAbsenceAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.DomainEvent
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.TemporaryAbsenceAuthorisationNotesChanged
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.NotesAction

data class AmendAuthorisationNotes(
  override val notes: String,
  override val reason: String? = null,
) : AuthorisationAction,
  NotesAction {
  override fun domainEvent(taa: TemporaryAbsenceAuthorisation): DomainEvent<*> = TemporaryAbsenceAuthorisationNotesChanged(taa.person.identifier, taa.id)
}
