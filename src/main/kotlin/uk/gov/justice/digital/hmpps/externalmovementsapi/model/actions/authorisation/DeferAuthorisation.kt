package uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.authorisation

import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.authorisation.TemporaryAbsenceAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.DomainEvent
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.TemporaryAbsenceAuthorisationDeferred

data class DeferAuthorisation(
  override val reason: String? = null,
) : AuthorisationAction {
  override fun domainEvent(taa: TemporaryAbsenceAuthorisation): DomainEvent<*> = TemporaryAbsenceAuthorisationDeferred(taa.person.identifier, taa.id)
}
