package uk.gov.justice.digital.hmpps.externalmovementsapi.sync

import com.fasterxml.jackson.annotation.JsonIgnore
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.ReferenceDataDomain.Code.ABSENCE_REASON
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.ReferenceDataDomain.Code.ABSENCE_SUB_TYPE
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.ReferenceDataDomain.Code.ABSENCE_TYPE
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.ReferenceDataDomain.Code.TAP_AUTHORISATION_STATUS
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.TapAuthorisationStatus.Code.APPROVED
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.TapAuthorisationStatus.Code.PENDING
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class TapApplicationRequest(
  val id: UUID?,
  val movementApplicationId: Long,
  val eventSubType: String,
  val applicationDate: LocalDate,
  val applicationStatus: String,
  val comment: String?,
  val prisonId: String?,
  val contactPersonName: String?,
  val applicationType: String,
  val temporaryAbsenceType: String?,
  val temporaryAbsenceSubType: String?,
  val audit: NomisAudit,
) {
  @JsonIgnore
  fun isRepeating() = applicationType == "REPEATING"

  @JsonIgnore
  val tapAuthStatusCode = when (applicationStatus) {
    "APP-SCH", "APP-UNSCH" -> APPROVED
    "PEN" -> PENDING
    else -> throw IllegalArgumentException("Unexpected application status")
  }

  fun requiredReferenceData() = listOfNotNull(
    TAP_AUTHORISATION_STATUS to tapAuthStatusCode.name,
    ABSENCE_REASON to eventSubType,
    temporaryAbsenceType?.let { ABSENCE_TYPE to it },
    temporaryAbsenceSubType?.let { ABSENCE_SUB_TYPE to it },
  )

  @JsonIgnore
  val approvedAt = if (tapAuthStatusCode == APPROVED) {
    audit.modifyDatetime ?: audit.createDatetime
  } else {
    null
  }

  @JsonIgnore
  val approvedBy = if (tapAuthStatusCode == APPROVED) {
    audit.modifyUserId ?: audit.createUsername
  } else {
    null
  }
}

data class NomisAudit(
  val createDatetime: LocalDateTime,
  val createUsername: String,
  val modifyUserId: String? = null,
  val modifyDatetime: LocalDateTime? = null,
  val auditTimestamp: LocalDateTime? = null,
  val auditUserId: String? = null,
)
