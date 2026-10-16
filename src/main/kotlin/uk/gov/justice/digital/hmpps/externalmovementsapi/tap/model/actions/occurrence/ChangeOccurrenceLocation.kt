package uk.gov.justice.digital.hmpps.externalmovementsapi.tap.model.actions.occurrence

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.validation.Valid
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.DomainEvent
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.TemporaryAbsenceRelocated
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.action.LocationAction
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.location.Location
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.domain.occurrence.TemporaryAbsenceOccurrence

data class ChangeOccurrenceLocation(
  @Valid override val location: Location,
  @JsonIgnore override val ignoreTruncation: Boolean = false,
) : OccurrenceAction,
  LocationAction {
  override fun domainEvent(tao: TemporaryAbsenceOccurrence): DomainEvent<*> = TemporaryAbsenceRelocated(tao.person.identifier, tao.id)
}
