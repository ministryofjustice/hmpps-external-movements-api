package uk.gov.justice.digital.hmpps.externalmovementsapi.model.paged

import uk.gov.justice.digital.hmpps.externalmovementsapi.model.Person
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.location.Location
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.referencedata.CodedDescription
import java.time.LocalDateTime
import java.util.UUID

data class TapOccurrenceSearchResponse(
  override val content: List<TapOccurrenceResult>,
  override val metadata: PageMetadata,
) : PagedResponse<TapOccurrenceResult>

data class TapOccurrenceResult(
  val id: UUID,
  val authorisation: TapOccurrenceAuthorisation,
  val status: CodedDescription,
  val releaseAt: LocalDateTime,
  val returnBy: LocalDateTime,
  val accompaniedBy: CodedDescription,
  val transport: CodedDescription,
  val location: Location,
  val isCancelled: Boolean,
)

data class TapOccurrenceAuthorisation(
  val id: UUID,
  val person: Person,
  val status: CodedDescription,
  val absenceType: CodedDescription?,
  val absenceSubType: CodedDescription?,
  val absenceReasonCategory: CodedDescription?,
  val absenceReason: CodedDescription?,
  val repeat: Boolean,
)
