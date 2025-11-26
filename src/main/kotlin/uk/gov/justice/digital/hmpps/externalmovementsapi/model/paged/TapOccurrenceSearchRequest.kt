package uk.gov.justice.digital.hmpps.externalmovementsapi.model.paged

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.validation.constraints.NotBlank
import org.springframework.data.domain.Sort
import org.springframework.data.domain.Sort.Direction
import org.springframework.data.domain.Sort.by
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.authorisation.TemporaryAbsenceAuthorisation.Companion.PERSON
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.occurrence.TemporaryAbsenceOccurrence.Companion.AUTHORISATION
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.occurrence.TemporaryAbsenceOccurrence.Companion.RELEASE_AT
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.occurrence.TemporaryAbsenceOccurrence.Companion.RETURN_BY
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.occurrence.TemporaryAbsenceOccurrence.Companion.STATUS
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.person.PersonSummary.Companion.FIRST_NAME
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.person.PersonSummary.Companion.IDENTIFIER
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.person.PersonSummary.Companion.LAST_NAME
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceData.Companion.SEQUENCE_NUMBER
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.TapOccurrenceStatus
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.removeNullChar
import java.time.LocalDate

data class TapOccurrenceSearchRequest(
  @NotBlank
  val prisonCode: String,
  val fromDate: LocalDate?,
  val toDate: LocalDate?,
  val status: Set<TapOccurrenceStatus.Code> = emptySet(),
  val query: String? = null,
  override val page: Int = 1,
  override val size: Int = 10,
  override val sort: String = RELEASE_AT,
) : PagedRequest {
  override fun validSortFields(): Set<String> = setOf(RELEASE_AT, RETURN_BY, STATUS)

  private fun sortByDate(direction: Direction, first: String, second: String) = by(direction, first, second).and(sortByPersonName())

  private fun sortByPersonName() = by(
    Direction.ASC,
    "${AUTHORISATION}_${PERSON}_${LAST_NAME}",
    "${AUTHORISATION}_${PERSON}_${FIRST_NAME}",
    "${AUTHORISATION}_${PERSON}_$IDENTIFIER",
  )

  @JsonIgnore
  val queryString: String? = query?.removeNullChar()?.takeIf { it.isNotBlank() }

  override fun buildSort(field: String, direction: Direction): Sort = when (field) {
    RELEASE_AT -> sortByDate(direction, RELEASE_AT, RETURN_BY)
    RETURN_BY -> sortByDate(direction, RETURN_BY, RELEASE_AT)
    STATUS -> by(direction, "${STATUS}_${SEQUENCE_NUMBER}")
    else -> throw IllegalArgumentException("Unrecognised sort field")
  }
}
