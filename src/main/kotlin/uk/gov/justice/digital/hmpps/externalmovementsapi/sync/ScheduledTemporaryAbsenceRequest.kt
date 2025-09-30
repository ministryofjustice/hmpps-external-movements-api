package uk.gov.justice.digital.hmpps.externalmovementsapi.sync

import com.fasterxml.jackson.annotation.JsonIgnore
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.ReferenceDataDomain.Code.ACCOMPANIED_BY
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.ReferenceDataDomain.Code.LOCATION_TYPE
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.ReferenceDataDomain.Code.TRANSPORT
import java.time.LocalDateTime
import java.util.UUID

data class ScheduledTemporaryAbsenceRequest(
  val id: UUID?,
  val eventId: Long,
  val eventStatus: String,
  val startTime: LocalDateTime,
  val returnTime: LocalDateTime,
  val toAddressOwnerClass: String?,
  val toAddressId: Long?,
  val contactPersonName: String?,
  val escort: String?,
  val comment: String?,
  val transportType: String?,
  @Schema(description = "Audit data associated with the records")
  val audit: NomisAudit,
) {
  @JsonIgnore
  val cancelled = eventStatus in listOf("CANC", "DEN")

  fun requiredReferenceData() = listOfNotNull(
    ACCOMPANIED_BY to escortOrDefault(),
    TRANSPORT to transportTypeOrDefault(),
    LOCATION_TYPE to (toAddressOwnerClass ?: "OTHER"),
  )

  @JsonIgnore
  val cancelledAt = if (cancelled) {
    audit.modifyDatetime ?: audit.createDatetime
  } else {
    null
  }

  @JsonIgnore
  val cancelledBy = if (cancelled) {
    audit.modifyUserId ?: audit.createUsername
  } else {
    null
  }

  fun escortOrDefault(): String = escort ?: "U"
  fun transportTypeOrDefault(): String = transportType ?: "TNR"
}
