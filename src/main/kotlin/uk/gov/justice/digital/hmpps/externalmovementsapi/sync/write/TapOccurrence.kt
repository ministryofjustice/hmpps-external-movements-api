package uk.gov.justice.digital.hmpps.externalmovementsapi.sync.write

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ACCOMPANIED_BY
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.TRANSPORT
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataRequired
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.of
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.location.Location
import uk.gov.justice.digital.hmpps.externalmovementsapi.sync.AtAndBy
import java.time.LocalDateTime
import java.util.UUID

@Schema(name = "SyncWriteTapOccurrence")
data class TapOccurrence(
  val id: UUID?,
  val statusCode: String,
  val releaseAt: LocalDateTime,
  val returnBy: LocalDateTime,
  val location: Location,
  val accompaniedByCode: String,
  val transportCode: String,
  val notes: String?,
  val added: AtAndBy,
  val cancelled: AtAndBy?,
  val legacyId: Long,
) : ReferenceDataRequired {
  override fun requiredReferenceData() = setOfNotNull(
    ACCOMPANIED_BY of accompaniedByCode,
    TRANSPORT of transportCode,
  )
}
