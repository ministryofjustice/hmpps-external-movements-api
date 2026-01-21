package uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.movement

data class ChangeMovementReason(
  val absenceReasonCode: String,
  override val reason: String? = null,
) : MovementAction
