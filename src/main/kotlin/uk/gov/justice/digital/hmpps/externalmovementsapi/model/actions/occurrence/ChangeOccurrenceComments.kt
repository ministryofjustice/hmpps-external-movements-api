package uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.occurrence

import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.occurrence.TemporaryAbsenceOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.DomainEvent
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.TemporaryAbsenceCommentsChanged
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.CommentsAction

data class ChangeOccurrenceComments(
  override val comments: String,
  override val reason: String? = null,
) : OccurrenceAction,
  CommentsAction {
  override fun domainEvent(tao: TemporaryAbsenceOccurrence): DomainEvent<*> = TemporaryAbsenceCommentsChanged(tao.authorisation.person.identifier, tao.id)
}
