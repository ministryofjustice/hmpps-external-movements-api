package uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.movement

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.JsonTypeInfo
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.movement.TemporaryAbsenceMovement
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.DomainEvent
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.Action

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
  value = [
    Type(value = ChangeMovementDirection::class, name = "ChangeMovementDirection"),
    Type(value = ChangeMovementAccompaniment::class, name = "ChangeMovementAccompaniment"),
    Type(value = ChangeMovementLocation::class, name = "ChangeMovementLocation"),
    Type(value = ChangeMovementComments::class, name = "ChangeMovementComments"),
    Type(value = ChangeMovementOccurredAt::class, name = "ChangeMovementOccurredAt"),
    Type(value = ChangeMovementReason::class, name = "ChangeMovementReason"),
  ],
)
sealed interface MovementAction : Action {
  fun domainEvent(tam: TemporaryAbsenceMovement): DomainEvent<*>? = null
}
