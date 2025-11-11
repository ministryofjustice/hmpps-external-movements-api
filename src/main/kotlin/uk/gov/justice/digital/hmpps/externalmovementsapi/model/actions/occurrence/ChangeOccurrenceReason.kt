package uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.occurrence

import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.ReasonPath

data class ChangeOccurrenceReason(
  val absenceTypeCode: String?,
  val absenceSubTypeCode: String?,
  val absenceReasonCategoryCode: String?,
  val absenceReasonCode: String?,
  val reasonPath: ReasonPath,
  override val reason: String? = null,
) : OccurrenceAction
