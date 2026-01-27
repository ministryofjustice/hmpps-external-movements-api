package uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions

import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.ReasonPath
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataKey
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataRequired
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.of
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.CategorisedAbsenceReason

interface AbsenceCategorisationAction :
  Action,
  ReferenceDataRequired {
  val absenceTypeCode: String?
  val absenceSubTypeCode: String?
  val absenceReasonCategoryCode: String?
  val absenceReasonCode: String?
  val reasonPath: ReasonPath

  fun changes(ac: CategorisedAbsenceReason): Boolean = ac.absenceType?.code != absenceTypeCode ||
    ac.absenceSubType?.code != absenceSubTypeCode ||
    ac.absenceReasonCategory?.code != absenceReasonCategoryCode ||
    ac.absenceReason.code != absenceReasonCode

  override fun requiredReferenceData(): Set<ReferenceDataKey> = setOfNotNull(
    absenceTypeCode?.let { ReferenceDataDomain.Code.ABSENCE_TYPE of it },
    absenceSubTypeCode?.let { ReferenceDataDomain.Code.ABSENCE_SUB_TYPE of it },
    absenceReasonCategoryCode?.let { ReferenceDataDomain.Code.ABSENCE_REASON_CATEGORY of it },
    absenceReasonCode?.let { ReferenceDataDomain.Code.ABSENCE_REASON of it },
  )
}

fun AbsenceCategorisationAction.reasonPath() = listOfNotNull(
  absenceTypeCode?.let { ReferenceDataDomain.Code.ABSENCE_TYPE of it },
  absenceSubTypeCode?.let { ReferenceDataDomain.Code.ABSENCE_SUB_TYPE of it },
  absenceReasonCategoryCode?.let { ReferenceDataDomain.Code.ABSENCE_REASON_CATEGORY of it },
  absenceReasonCode?.let { ReferenceDataDomain.Code.ABSENCE_REASON of it },
)
