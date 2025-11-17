package uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.authorisation

import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.authorisation.TemporaryAbsenceAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.DomainEvent
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.TemporaryAbsenceAuthorisationDenied

data class DenyAuthorisation(
  override val reason: String? = null,
) : AuthorisationAction {
  override fun domainEvent(taa: TemporaryAbsenceAuthorisation): DomainEvent<*> = TemporaryAbsenceAuthorisationDenied(taa.personIdentifier, taa.id)
}
