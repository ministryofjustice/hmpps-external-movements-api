package uk.gov.justice.digital.hmpps.externalmovementsapi.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.databind.JsonNode
import jakarta.validation.Valid
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.ExternalMovementContext
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_REASON
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_SUB_TYPE
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_TYPE
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ACCOMPANIED_BY
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.TAP_AUTHORISATION_STATUS
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.TRANSPORT
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.TapAuthorisationStatus
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.TapAuthorisationStatus.Code.APPROVED
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.location.Location
import java.time.LocalDate
import java.time.LocalDateTime

data class CreateTapAuthorisationRequest(
  val absenceTypeCode: String,
  val absenceSubTypeCode: String?,
  val absenceReasonCode: String?,
  @Valid
  val occurrences: List<CreateTapOccurrenceRequest>,
  val statusCode: TapAuthorisationStatus.Code,
  val notes: String?,
  val repeat: Boolean,
  val fromDate: LocalDate,
  val toDate: LocalDate,
  @JsonIgnore
  val applicationDate: LocalDate = LocalDate.now(),
  @JsonIgnore
  val submittedBy: String = ExternalMovementContext.get().username,
  @JsonIgnore
  val approvedAt: LocalDateTime? = if (statusCode == APPROVED) LocalDateTime.now() else null,
  @JsonIgnore
  val approvedBy: String? = if (statusCode == APPROVED) ExternalMovementContext.get().username else null,
  val schedule: JsonNode? = null,
) {
  fun requiredReferenceData() = buildSet {
    add(TAP_AUTHORISATION_STATUS to statusCode.name)
    add(ABSENCE_TYPE to absenceTypeCode)
    absenceSubTypeCode?.also { add(ABSENCE_SUB_TYPE to it) }
    absenceReasonCode?.also { add(ABSENCE_REASON to it) }
    addAll(occurrences.flatMap(CreateTapOccurrenceRequest::requiredReferenceData))
  }
}

data class CreateTapOccurrenceRequest(
  val releaseAt: LocalDateTime,
  val returnBy: LocalDateTime,
  val accompaniedByCode: String,
  val transportCode: String,
  @Valid
  val location: Location,
  val contactInformation: String?,
  val notes: String?,
  val scheduleReference: JsonNode?,
) {
  fun requiredReferenceData() = listOfNotNull(
    ACCOMPANIED_BY to accompaniedByCode,
    TRANSPORT to transportCode,
  )
}
