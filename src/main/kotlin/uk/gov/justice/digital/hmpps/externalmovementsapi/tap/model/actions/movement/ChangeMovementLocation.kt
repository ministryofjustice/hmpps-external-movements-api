package uk.gov.justice.digital.hmpps.externalmovementsapi.tap.model.actions.movement

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.validation.Valid
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.DomainEvent
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.TapMovementRelocated
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.action.LocationAction
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.location.Location
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.domain.movement.TemporaryAbsenceMovement

data class ChangeMovementLocation(
  @Valid override val location: Location,
  @JsonIgnore override val ignoreTruncation: Boolean = false,
) : MovementAction,
  LocationAction {
  override fun domainEvent(tam: TemporaryAbsenceMovement): DomainEvent<*> = TapMovementRelocated(tam.person.identifier, tam.id)
}
