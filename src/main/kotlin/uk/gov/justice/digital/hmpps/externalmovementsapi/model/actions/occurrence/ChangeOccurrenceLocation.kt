package uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.occurrence

import jakarta.validation.Valid
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.occurrence.TemporaryAbsenceOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.DomainEvent
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.TemporaryAbsenceRelocated
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.location.Location

data class ChangeOccurrenceLocation(
  @Valid val location: Location,
  override val reason: String? = null,
) : OccurrenceAction {
  override fun domainEvent(tao: TemporaryAbsenceOccurrence): DomainEvent<*> = TemporaryAbsenceRelocated(tao.authorisation.person.identifier, tao.id)
}
