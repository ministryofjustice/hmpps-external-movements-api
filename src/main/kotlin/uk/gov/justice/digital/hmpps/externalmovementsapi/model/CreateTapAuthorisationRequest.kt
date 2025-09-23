package uk.gov.justice.digital.hmpps.externalmovementsapi.model

import com.fasterxml.jackson.annotation.JsonIgnore
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.ExternalMovementContext
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.ReferenceDataDomain.Code.ABSENCE_REASON
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.ReferenceDataDomain.Code.ABSENCE_SUB_TYPE
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.ReferenceDataDomain.Code.ABSENCE_TYPE
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.ReferenceDataDomain.Code.ACCOMPANIED_BY
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.ReferenceDataDomain.Code.LOCATION_TYPE
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.ReferenceDataDomain.Code.TAP_AUTHORISATION_STATUS
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.ReferenceDataDomain.Code.TRANSPORT
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.TapAuthorisationStatus
import java.time.LocalDate
import java.time.LocalDateTime

data class CreateTapAuthorisationRequest(
  val submittedAt: LocalDateTime,
  val repeat: Boolean,
  val approvalRequired: Boolean,
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
  @JsonIgnore
  val statusCode: String = (if (approvalRequired) TapAuthorisationStatus.Code.PENDING else TapAuthorisationStatus.Code.APPROVED).name,
  @JsonIgnore
  val applicationDate: LocalDate = LocalDate.now(),
  @JsonIgnore
  val submittedBy: String = ExternalMovementContext.get().username,
  @JsonIgnore
  val approvedAt: LocalDateTime? = if (approvalRequired) null else LocalDateTime.now(),
  @JsonIgnore
  val approvedBy: String? = if (approvalRequired) null else ExternalMovementContext.get().username,
) {
  fun requiredReferenceData() = listOfNotNull(
    TAP_AUTHORISATION_STATUS to statusCode,
    ABSENCE_TYPE to absenceTypeCode,
    absenceSubTypeCode?.let { ABSENCE_SUB_TYPE to it },
    absenceReasonCode?.let { ABSENCE_REASON to it },
    accompaniedByCode?.let { ACCOMPANIED_BY to it },
    transportCode?.let { TRANSPORT to it },
    LOCATION_TYPE to locationTypeCode,
  )
}
