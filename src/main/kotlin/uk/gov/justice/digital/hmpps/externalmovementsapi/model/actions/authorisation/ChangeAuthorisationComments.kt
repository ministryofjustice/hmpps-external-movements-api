package uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.authorisation

import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.authorisation.TemporaryAbsenceAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.DomainEvent
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.TemporaryAbsenceAuthorisationCommentsChanged
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.CommentsAction

data class ChangeAuthorisationComments(
  override val comments: String,
  override val reason: String? = null,
) : AuthorisationAction,
  CommentsAction {
  override fun domainEvent(taa: TemporaryAbsenceAuthorisation): DomainEvent<*> = TemporaryAbsenceAuthorisationCommentsChanged(taa.person.identifier, taa.id)
}
