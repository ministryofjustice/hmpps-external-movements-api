package uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence

import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.ReasonPath
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.AbsenceReason
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.AbsenceReasonCategory
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.AbsenceSubType
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.AbsenceType
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain

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
