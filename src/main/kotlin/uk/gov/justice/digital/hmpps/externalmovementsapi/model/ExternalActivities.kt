package uk.gov.justice.digital.hmpps.externalmovementsapi.model

import uk.gov.justice.digital.hmpps.externalmovementsapi.model.referencedata.CodedDescription
import java.time.LocalDateTime
import java.util.UUID

data class ExternalActivities(
  val content: List<ExternalActivity>,
)

data class ExternalActivity(
  val id: UUID,
  val personIdentifier: String,
  val description: ScheduledMovementDescription,
  val start: LocalDateTime,
  val end: LocalDateTime,
  val status: CodedDescription,
  val detail: ScheduledMovementDetail,
)
