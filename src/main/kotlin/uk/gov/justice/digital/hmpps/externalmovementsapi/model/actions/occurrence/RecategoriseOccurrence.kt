package uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.occurrence

import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.ReasonPath
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.AbsenceCategorisationAction

data class RecategoriseOccurrence(
  override val absenceTypeCode: String?,
  override val absenceSubTypeCode: String?,
  override val absenceReasonCategoryCode: String?,
  override val absenceReasonCode: String?,
  override val reasonPath: ReasonPath,
  override val reason: String? = null,
) : OccurrenceAction,
  AbsenceCategorisationAction
