package uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap

import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.ReasonPath
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.absencereason.AbsenceReason
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.absencereason.AbsenceReasonCategory
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.absencereason.AbsenceSubType
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.absencereason.AbsenceType

interface CategorisedAbsenceReason {
  val absenceType: AbsenceType?
  val absenceSubType: AbsenceSubType?
  val absenceReasonCategory: AbsenceReasonCategory?
  val absenceReason: AbsenceReason?

  fun hierarchyDescription(reasonPath: ReasonPath): String = listOfNotNull(
    absenceType?.takeIf { reasonPath.has(ReferenceDataDomain.Code.ABSENCE_TYPE) },
    absenceSubType?.takeIf { reasonPath.has(ReferenceDataDomain.Code.ABSENCE_SUB_TYPE) },
    absenceReasonCategory?.takeIf { reasonPath.has(ReferenceDataDomain.Code.ABSENCE_REASON_CATEGORY) },
    absenceReason?.takeIf { reasonPath.has(ReferenceDataDomain.Code.ABSENCE_REASON) },
  ).joinToString(" > ")
}
