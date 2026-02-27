package uk.gov.justice.digital.hmpps.externalmovementsapi.model.paged

import com.fasterxml.jackson.annotation.JsonIgnore
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.occurrence.TemporaryAbsenceOccurrence.Companion.START
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.OccurrenceStatus
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.PersonIdentifierDateRange
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.ValidPersonIdentifierOrDateRange
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.ValidStartAndEnd
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.removeNullChar
import java.time.LocalDate

@ValidStartAndEnd
@ValidPersonIdentifierOrDateRange
data class TapOccurrenceSearchRequest(
  val prisonCode: String,
  override val start: LocalDate? = null,
  override val end: LocalDate? = null,
  val status: Set<OccurrenceStatus.Code> = emptySet(),
  val absenceCategorisation: AbsenceCategorisationFilter? = null,
  override val query: String? = null,
  override val page: Int = 1,
  override val size: Int = 10,
  override val sort: String = START,
) : OccurrenceSearchRequest,
  PersonIdentifierDateRange<LocalDate> {
  @JsonIgnore
  val queryString: String? = query?.removeNullChar()?.takeIf { it.isNotBlank() }
}
