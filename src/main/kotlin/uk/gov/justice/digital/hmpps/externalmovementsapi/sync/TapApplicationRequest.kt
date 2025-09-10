package uk.gov.justice.digital.hmpps.externalmovementsapi.sync

import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.ReferenceDataDomain.Code.ABSENCE_REASON
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.ReferenceDataDomain.Code.ABSENCE_SUB_TYPE
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.ReferenceDataDomain.Code.ABSENCE_TYPE
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.ReferenceDataDomain.Code.ACCOMPANIED_BY
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.ReferenceDataDomain.Code.LOCATION_TYPE
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.ReferenceDataDomain.Code.TAP_STATUS
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.ReferenceDataDomain.Code.TRANSPORT
import java.time.LocalDate
import java.time.LocalDateTime

data class TapApplicationRequest(
  val movementApplicationId: Long,
  val eventSubType: String,
  val applicationDate: LocalDate,
  val fromDate: LocalDate,
  val releaseTime: LocalDateTime,
  val toDate: LocalDate,
  val returnTime: LocalDateTime,
  val applicationStatus: String,
  val escortCode: String?,
  val transportType: String?,
  val comment: String?,
  val prisonId: String?,
  val toAgencyId: String?,
  val toAddressId: Long?,
  val toAddressOwnerClass: String?,
  val contactPersonName: String?,
  val applicationType: String,
  val temporaryAbsenceType: String?,
  val temporaryAbsenceSubType: String?,
  val audit: NomisAudit,
) {
  fun isRepeating() = applicationType == "REPEATING"
  fun isAccompanied() = escortCode != "U"

  fun requiredReferenceData() = listOfNotNull(
    TAP_STATUS to applicationStatus,
    ABSENCE_REASON to eventSubType,
    temporaryAbsenceType?.let { ABSENCE_TYPE to it },
    temporaryAbsenceSubType?.let { ABSENCE_SUB_TYPE to it },
    escortCode?.let { ACCOMPANIED_BY to it },
    transportType?.let { TRANSPORT to it },
    toAddressOwnerClass?.let { LOCATION_TYPE to it },
    LOCATION_TYPE to "OTHER",
  )
}

data class NomisAudit(
  val createDatetime: LocalDateTime,
  val createUsername: String,
  val modifyUserId: String? = null,
  val modifyDatetime: LocalDateTime? = null,
  val auditTimestamp: LocalDateTime? = null,
  val auditUserId: String? = null,
)
