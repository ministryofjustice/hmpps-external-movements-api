package uk.gov.justice.digital.hmpps.externalmovementsapi.model.tapseries

import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.ReferenceDataDomain.Code.ABSENCE_REASON
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.ReferenceDataDomain.Code.ABSENCE_SUB_TYPE
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.ReferenceDataDomain.Code.ABSENCE_TYPE
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.ReferenceDataDomain.Code.ACCOMPANIED_BY
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.ReferenceDataDomain.Code.LOCATION_TYPE
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.ReferenceDataDomain.Code.TAP_STATUS
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.ReferenceDataDomain.Code.TRANSPORT
import java.time.LocalDateTime

data class CreateTapSeriesRequest(
  val submittedAt: LocalDateTime,
  val repeat: Boolean,
  val statusCode: String,
  val absenceTypeCode: String,
  val absenceSubTypeCode: String?,
  val absenceReasonCode: String?,
  val releaseAt: LocalDateTime,
  val returnBy: LocalDateTime,
  val accompanied: Boolean,
  val accompaniedByCode: String?,
  val transportCode: String?,
  val notes: String?,
  val locationTypeCode: String,
  val locationId: String?,
) {
  fun requiredReferenceData() = listOfNotNull(
    TAP_STATUS to statusCode,
    ABSENCE_TYPE to absenceTypeCode,
    absenceSubTypeCode?.let { ABSENCE_SUB_TYPE to it },
    absenceReasonCode?.let { ABSENCE_REASON to it },
    accompaniedByCode?.let { ACCOMPANIED_BY to it },
    transportCode?.let { TRANSPORT to it },
    LOCATION_TYPE to locationTypeCode,
  )
}
