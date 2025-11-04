package uk.gov.justice.digital.hmpps.externalmovementsapi.sync.write

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.TemporaryAbsenceMovement
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_REASON
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ACCOMPANIED_BY
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataRequired
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.of
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.location.Location
import java.time.LocalDateTime
import java.util.UUID

@Schema(name = "SyncWriteTapMovement")
data class TapMovement(
  val id: UUID?,
  val occurrenceId: UUID?,
  val occurredAt: LocalDateTime,
  val direction: TemporaryAbsenceMovement.Direction,
  val absenceReasonCode: String,
  val location: Location,
  val accompaniedByCode: String,
  val accompaniedByNotes: String?,
  val notes: String?,
  val recorded: AtAndByWithPrison,
  val legacyId: String,
) : ReferenceDataRequired {
  @Schema(name = "SyncAtAndByWithPrison")
  data class AtAndByWithPrison(
    val at: LocalDateTime,
    val by: String,
    val prisonCode: String,
  )

  override fun requiredReferenceData() = setOfNotNull(
    ABSENCE_REASON of absenceReasonCode,
    ACCOMPANIED_BY of accompaniedByCode,
  )
}
