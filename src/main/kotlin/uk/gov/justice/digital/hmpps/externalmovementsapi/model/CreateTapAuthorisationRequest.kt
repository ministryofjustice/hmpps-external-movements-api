package uk.gov.justice.digital.hmpps.externalmovementsapi.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.databind.JsonNode
import jakarta.validation.Valid
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.ExternalMovementContext
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_REASON
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_REASON_CATEGORY
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_SUB_TYPE
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_TYPE
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ACCOMPANIED_BY
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.TAP_AUTHORISATION_STATUS
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.TRANSPORT
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataKey
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataRequired
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.TapAuthorisationStatus
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.TapAuthorisationStatus.Code.APPROVED
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.of
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.location.Location
import java.time.LocalDate
import java.time.LocalDateTime

data class CreateTapAuthorisationRequest(
  val absenceTypeCode: String,
  val absenceSubTypeCode: String?,
  val absenceReasonCategoryCode: String?,
  val absenceReasonCode: String?,
  @Valid
  val occurrences: List<CreateTapOccurrenceRequest>,
  val statusCode: TapAuthorisationStatus.Code,
  val accompaniedByCode: String = occurrences.firstOrNull()?.accompaniedByCode ?: "U",
  val transportCode: String = occurrences.firstOrNull()?.transportCode ?: "TNR",
  val notes: String?,
  val repeat: Boolean,
  val fromDate: LocalDate,
  val toDate: LocalDate,
  val contactInformation: String?,
  @JsonIgnore
  val submittedAt: LocalDateTime = ExternalMovementContext.get().requestAt,
  @JsonIgnore
  val submittedBy: String = ExternalMovementContext.get().username,
  @JsonIgnore
  val approvedAt: LocalDateTime? = if (statusCode == APPROVED) ExternalMovementContext.get().requestAt else null,
  @JsonIgnore
  val approvedBy: String? = if (statusCode == APPROVED) ExternalMovementContext.get().username else null,
  val schedule: JsonNode? = null,
) : ReferenceDataRequired {
  override fun requiredReferenceData() = buildSet {
    addAll(reasonPath())
    add(TAP_AUTHORISATION_STATUS of statusCode.name)
    add(ACCOMPANIED_BY of accompaniedByCode)
    add(TRANSPORT of transportCode)
    addAll(occurrences.flatMap(CreateTapOccurrenceRequest::requiredReferenceData))
  }

  fun reasonPath(): List<ReferenceDataKey> = buildList {
    add(ABSENCE_TYPE of absenceTypeCode)
    absenceSubTypeCode?.also { add(ABSENCE_SUB_TYPE of it) }
    absenceReasonCategoryCode?.also { add(ABSENCE_REASON_CATEGORY of it) }
    absenceReasonCode?.also { add(ABSENCE_REASON of it) }
  }
}

data class CreateTapOccurrenceRequest(
  val absenceTypeCode: String?,
  val absenceSubTypeCode: String?,
  val absenceReasonCategoryCode: String?,
  val absenceReasonCode: String?,
  val releaseAt: LocalDateTime,
  val returnBy: LocalDateTime,
  val accompaniedByCode: String?,
  val transportCode: String?,
  @Valid
  val location: Location,
  val contactInformation: String?,
  val notes: String?,
  val scheduleReference: JsonNode?,
) : ReferenceDataRequired {
  override fun requiredReferenceData() = buildSet {
    accompaniedByCode?.also { add(ACCOMPANIED_BY of it) }
    transportCode?.also { add(TRANSPORT of it) }
    absenceTypeCode?.also { add(ABSENCE_TYPE of it) }
    absenceSubTypeCode?.also { add(ABSENCE_SUB_TYPE of it) }
    absenceReasonCategoryCode?.also { add(ABSENCE_REASON_CATEGORY of it) }
    absenceReasonCode?.also { add(ABSENCE_REASON of it) }
  }
}
