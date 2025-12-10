package uk.gov.justice.digital.hmpps.externalmovementsapi.model.paged

import uk.gov.justice.digital.hmpps.externalmovementsapi.model.Person
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.location.Location
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.referencedata.CodedDescription
import java.time.LocalDate
import java.util.UUID

data class TapAuthorisationSearchResponse(
  override val content: List<TapAuthorisationResult>,
  override val metadata: PageMetadata,
) : PagedResponse<TapAuthorisationResult>

data class TapAuthorisationResult(
  val id: UUID,
  val person: Person,
  val status: CodedDescription,
  val absenceType: CodedDescription?,
  val absenceSubType: CodedDescription?,
  val absenceReasonCategory: CodedDescription?,
  val absenceReason: CodedDescription?,
  val repeat: Boolean,
  val start: LocalDate,
  val end: LocalDate,
  val locations: List<Location>,
  val occurrenceCount: Int,
) {
  val fromDate: LocalDate = start
  val toDate: LocalDate = end
}
