package uk.gov.justice.digital.hmpps.externalmovementsapi.tap.domain

import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.domain.referencedata.absencereason.AbsenceReason
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.domain.referencedata.absencereason.AbsenceReasonCategory
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.domain.referencedata.absencereason.AbsenceSubType
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.domain.referencedata.absencereason.AbsenceType

interface CategorisedAbsenceReason {
  val absenceType: AbsenceType?
  val absenceSubType: AbsenceSubType?
  val absenceReasonCategory: AbsenceReasonCategory?
  val absenceReason: AbsenceReason

  fun hierarchyDescription(reasonPath: ReasonPath): String = listOfNotNull(
    absenceType?.takeIf { reasonPath.has(ReferenceDataDomain.Code.ABSENCE_TYPE) }?.description,
    absenceSubType?.takeIf { reasonPath.has(ReferenceDataDomain.Code.ABSENCE_SUB_TYPE) }?.description,
    absenceReasonCategory?.takeIf { reasonPath.has(ReferenceDataDomain.Code.ABSENCE_REASON_CATEGORY) }?.description,
    absenceReason.takeIf { reasonPath.has(ReferenceDataDomain.Code.ABSENCE_REASON) }?.description,
  ).joinToString(" > ")

  fun shortDescription(): String = absenceReasonCategory?.description ?: absenceReason.description
}
