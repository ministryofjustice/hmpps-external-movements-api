package uk.gov.justice.digital.hmpps.externalmovementsapi.tap.model.actions.authorisation

import uk.gov.justice.digital.hmpps.externalmovementsapi.events.DomainEvent
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.TemporaryAbsenceAuthorisationApproved
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.domain.authorisation.TemporaryAbsenceAuthorisation

data class ApproveAuthorisation(
  override val reason: String? = null,
) : AuthorisationAction {
  override fun domainEvent(taa: TemporaryAbsenceAuthorisation): DomainEvent<*> = TemporaryAbsenceAuthorisationApproved(taa.person.identifier, taa.id)
}
