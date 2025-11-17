package uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.authorisation

import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.NotesAction

data class AmendAuthorisationNotes(
  override val notes: String,
  override val reason: String? = null,
) : AuthorisationAction,
  NotesAction
