package uk.gov.justice.digital.hmpps.externalmovementsapi.domain

import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataKey
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.of

data class ReasonPath(val path: List<ReferenceDataKey>) {
  constructor(
    absenceTypeCode: String?,
    absenceSubTypeCode: String?,
    absenceReasonCategoryCode: String?,
    absenceReasonCode: String?,
  ) : this(
    listOfNotNull(
      absenceTypeCode?.let { ReferenceDataDomain.Code.ABSENCE_TYPE of it },
      absenceSubTypeCode?.let { ReferenceDataDomain.Code.ABSENCE_SUB_TYPE of it },
      absenceReasonCategoryCode?.let { ReferenceDataDomain.Code.ABSENCE_REASON_CATEGORY of it },
      absenceReasonCode?.let { ReferenceDataDomain.Code.ABSENCE_REASON of it },
    ),
  )

  fun has(domain: ReferenceDataDomain.Code): Boolean = path.any { it.domain == domain }
}
