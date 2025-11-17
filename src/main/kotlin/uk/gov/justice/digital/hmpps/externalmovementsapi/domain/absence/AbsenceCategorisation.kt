package uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence

import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.AbsenceReason
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.AbsenceReasonCategory
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.AbsenceSubType
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.AbsenceType

interface AbsenceCategorisation {
  val absenceType: AbsenceType?
  val absenceSubType: AbsenceSubType?
  val absenceReasonCategory: AbsenceReasonCategory?
  val absenceReason: AbsenceReason?
}
