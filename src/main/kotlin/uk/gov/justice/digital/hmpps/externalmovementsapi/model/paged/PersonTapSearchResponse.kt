package uk.gov.justice.digital.hmpps.externalmovementsapi.model.paged

import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.prisonregister.Prison
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.location.Location
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.referencedata.CodedDescription
import java.time.LocalDateTime
import java.util.UUID

data class PersonTapSearchResponse(
  override val content: List<PersonOccurrenceResult>,
  override val metadata: PageMetadata,
) : PagedResponse<PersonOccurrenceResult>

data class PersonOccurrenceResult(
  val id: UUID,
  val authorisation: PersonOccurrenceAuthorisation,
  val prison: Prison,
  val status: CodedDescription,
  val absenceType: CodedDescription?,
  val absenceSubType: CodedDescription?,
  val absenceReasonCategory: CodedDescription?,
  val absenceReason: CodedDescription?,
  val start: LocalDateTime,
  val end: LocalDateTime,
  val accompaniedBy: CodedDescription,
  val transport: CodedDescription,
  val location: Location,
  val isCancelled: Boolean,
  val absenceCategorisation: String,
)

data class PersonOccurrenceAuthorisation(
  val id: UUID,
  val status: CodedDescription,
  val absenceType: CodedDescription?,
  val absenceSubType: CodedDescription?,
  val absenceReasonCategory: CodedDescription?,
  val absenceReason: CodedDescription?,
  val repeat: Boolean,
)
