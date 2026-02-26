package uk.gov.justice.digital.hmpps.externalmovementsapi.model.paged

import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.occurrence.TemporaryAbsenceOccurrence.Companion.START
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.OccurrenceStatus
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.StartAndEnd
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.ValidStartAndEnd
import java.time.LocalDate

@ValidStartAndEnd
data class PersonTapSearchRequest(
  override val start: LocalDate? = null,
  override val end: LocalDate? = null,
  val status: Set<OccurrenceStatus.Code> = emptySet(),
  val absenceCategorisation: AbsenceCategorisationFilter? = null,
  override val page: Int = 1,
  override val size: Int = 10,
  override val sort: String = START,
) : OccurrenceSearchRequest,
  StartAndEnd<LocalDate>
