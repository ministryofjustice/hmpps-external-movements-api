package uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.authorisation

import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.authorisation.TemporaryAbsenceAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.DomainEvent
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.TemporaryAbsenceAuthorisationRelocated
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.location.Location
import java.util.SequencedSet

data class ChangeAuthorisationLocations(
  val locations: SequencedSet<Location>,
  override val reason: String? = null,
) : AuthorisationAction {
  constructor(location: Location, reason: String? = null) : this(linkedSetOf(location))
  override fun domainEvent(taa: TemporaryAbsenceAuthorisation): DomainEvent<*> = TemporaryAbsenceAuthorisationRelocated(taa.person.identifier, taa.id)
}
