package uk.gov.justice.digital.hmpps.externalmovementsapi.tap.model.actions.authorisation

import uk.gov.justice.digital.hmpps.externalmovementsapi.events.DomainEvent
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.TemporaryAbsenceAuthorisationAccompanimentChanged
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.domain.authorisation.TemporaryAbsenceAuthorisation

data class ChangeAuthorisationAccompaniment(
  val accompaniedByCode: String,
  override val reason: String? = null,
) : AuthorisationAction {
  override fun domainEvent(taa: TemporaryAbsenceAuthorisation): DomainEvent<*> = TemporaryAbsenceAuthorisationAccompanimentChanged(taa.person.identifier, taa.id)
}
