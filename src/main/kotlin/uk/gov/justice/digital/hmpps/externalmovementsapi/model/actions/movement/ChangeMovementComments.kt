package uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.movement

import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.CommentsAction

data class ChangeMovementComments(
  override val comments: String,
  override val reason: String? = null,
) : MovementAction,
  CommentsAction
