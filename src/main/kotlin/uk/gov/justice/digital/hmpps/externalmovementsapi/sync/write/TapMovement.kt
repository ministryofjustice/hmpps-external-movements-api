package uk.gov.justice.digital.hmpps.externalmovementsapi.sync.write

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_REASON
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ACCOMPANIED_BY
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.TAP_OCCURRENCE_STATUS
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataRequired
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.of
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.movement.TemporaryAbsenceMovement
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.OccurrenceStatus
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.location.Location
import uk.gov.justice.digital.hmpps.externalmovementsapi.sync.AtAndBy
import java.time.LocalDateTime
import java.util.UUID

@Schema(name = "SyncWriteTapMovement")
data class TapMovement(
  val id: UUID?,
  val occurrenceId: UUID?,
  val occurredAt: LocalDateTime,
  val direction: TemporaryAbsenceMovement.Direction,
  val prisonCode: String,
  val absenceReasonCode: String,
  val location: Location,
  val accompaniedByCode: String,
  val accompaniedByComments: String?,
  val comments: String?,
  val created: AtAndBy,
  val updated: AtAndBy?,
  val legacyId: String,
) : ReferenceDataRequired {
  override fun requiredReferenceData() = setOfNotNull(
    ABSENCE_REASON of absenceReasonCode,
    ACCOMPANIED_BY of accompaniedByCode,
  ) + OccurrenceStatus.Code.entries.map { TAP_OCCURRENCE_STATUS of it.name }
}
