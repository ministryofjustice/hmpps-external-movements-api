package uk.gov.justice.digital.hmpps.externalmovementsapi.sync.write

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_REASON
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_SUB_TYPE
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_TYPE
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.TAP_AUTHORISATION_STATUS
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataRequired
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.of
import uk.gov.justice.digital.hmpps.externalmovementsapi.sync.AtAndBy
import java.time.LocalDate
import java.util.UUID

@Schema(name = "SyncWriteTapAuthorisation")
data class TapAuthorisation(
  val id: UUID?,
  val prisonCode: String,
  val statusCode: String,
  val absenceTypeCode: String?,
  val absenceSubTypeCode: String?,
  val absenceReasonCode: String,
  val repeat: Boolean,
  val fromDate: LocalDate,
  val toDate: LocalDate,
  val notes: String?,
  val created: AtAndBy,
  val updated: AtAndBy?,
  val legacyId: Long,
) : ReferenceDataRequired {
  override fun requiredReferenceData() = setOfNotNull(
    TAP_AUTHORISATION_STATUS of statusCode,
    ABSENCE_REASON of absenceReasonCode,
    absenceTypeCode?.let { ABSENCE_TYPE of it },
    absenceSubTypeCode?.let { ABSENCE_SUB_TYPE of it },
  )
}
