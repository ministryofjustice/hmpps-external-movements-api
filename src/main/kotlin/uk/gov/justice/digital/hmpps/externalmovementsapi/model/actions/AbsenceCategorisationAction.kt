package uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions

import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.ReasonPath
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.AbsenceCategorisation

interface AbsenceCategorisationAction : Action {
  val absenceTypeCode: String?
  val absenceSubTypeCode: String?
  val absenceReasonCategoryCode: String?
  val absenceReasonCode: String?
  val reasonPath: ReasonPath

  fun changes(ac: AbsenceCategorisation): Boolean = ac.absenceType?.code != absenceTypeCode ||
    ac.absenceSubType?.code != absenceSubTypeCode ||
    ac.absenceReasonCategory?.code != absenceReasonCategoryCode ||
    ac.absenceReason?.code != absenceReasonCode
}
