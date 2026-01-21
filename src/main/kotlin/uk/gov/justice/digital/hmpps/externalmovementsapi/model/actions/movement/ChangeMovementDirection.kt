package uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.movement

import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.movement.TemporaryAbsenceMovement

data class ChangeMovementDirection(
  val direction: TemporaryAbsenceMovement.Direction,
  override val reason: String? = null,
) : MovementAction
