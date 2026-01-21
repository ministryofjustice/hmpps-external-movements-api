package uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.movement

import jakarta.validation.Valid
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.location.Location

data class ChangeMovementLocation(
  @Valid val location: Location,
  override val reason: String? = null,
) : MovementAction
