package uk.gov.justice.digital.hmpps.externalmovementsapi.sync.write

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_REASON
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_SUB_TYPE
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_TYPE
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ACCOMPANIED_BY
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.TAP_OCCURRENCE_STATUS
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.TRANSPORT
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataRequired
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.TapOccurrenceStatus
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.of
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.location.Location
import uk.gov.justice.digital.hmpps.externalmovementsapi.sync.AtAndBy
import java.time.LocalDateTime
import java.util.UUID

@Schema(name = "SyncWriteTapOccurrence")
data class TapOccurrence(
  val id: UUID?,
  val isCancelled: Boolean,
  val start: LocalDateTime,
  val end: LocalDateTime,
  val location: Location,
  val absenceTypeCode: String?,
  val absenceSubTypeCode: String?,
  val absenceReasonCode: String,
  val accompaniedByCode: String,
  val transportCode: String,
  val contactInformation: String?,
  val comments: String?,
  val created: AtAndBy,
  val updated: AtAndBy?,
  val legacyId: Long,
) : ReferenceDataRequired {
  override fun requiredReferenceData() = setOfNotNull(
    ACCOMPANIED_BY of accompaniedByCode,
    TRANSPORT of transportCode,
    ABSENCE_REASON of absenceReasonCode,
    absenceTypeCode?.let { ABSENCE_TYPE of it },
    absenceSubTypeCode?.let { ABSENCE_SUB_TYPE of it },
  ) + TapOccurrenceStatus.Code.entries.map { TAP_OCCURRENCE_STATUS of it.name }
}
