package uk.gov.justice.digital.hmpps.externalmovementsapi.sync.write

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_REASON
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_SUB_TYPE
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_TYPE
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ACCOMPANIED_BY
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.TAP_AUTHORISATION_STATUS
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.TAP_OCCURRENCE_STATUS
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.TRANSPORT
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataRequired
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.of
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.AuthorisationStatus
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.OccurrenceStatus
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.location.Location
import uk.gov.justice.digital.hmpps.externalmovementsapi.sync.AtAndBy
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

@Schema(name = "SyncWriteTapAuthorisation")
data class TapAuthorisation(
  val id: UUID?,
  val prisonCode: String,
  val statusCode: String,
  val absenceTypeCode: String?,
  val absenceSubTypeCode: String?,
  val absenceReasonCode: String,
  val accompaniedByCode: String,
  val transportCode: String,
  val repeat: Boolean,
  val start: LocalDate,
  val end: LocalDate,
  val startTime: LocalTime,
  val endTime: LocalTime,
  val location: Location?,
  val comments: String?,
  val created: AtAndBy,
  val updated: AtAndBy?,
  val legacyId: Long,
) : ReferenceDataRequired {
  override fun requiredReferenceData() = setOfNotNull(
    TAP_AUTHORISATION_STATUS of statusCode,
    TAP_AUTHORISATION_STATUS of AuthorisationStatus.Code.EXPIRED.name,
    ABSENCE_REASON of absenceReasonCode,
    absenceTypeCode?.let { ABSENCE_TYPE of it },
    absenceSubTypeCode?.let { ABSENCE_SUB_TYPE of it },
    ACCOMPANIED_BY of accompaniedByCode,
    TRANSPORT of transportCode,
  ) + OccurrenceStatus.Code.entries.map { TAP_OCCURRENCE_STATUS of it.name }

  fun schedule(): AuthorisationSchedule? = if (!repeat) {
    AuthorisationSchedule(startTime, endTime)
  } else {
    null
  }
}
