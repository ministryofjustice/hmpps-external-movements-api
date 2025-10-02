package uk.gov.justice.digital.hmpps.externalmovementsapi.model.paged

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.validation.constraints.NotBlank
import org.springframework.data.domain.Sort
import org.springframework.data.domain.Sort.Direction
import org.springframework.data.domain.Sort.by
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.TemporaryAbsenceOccurrence.Companion.PERSON_IDENTIFIER
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.TemporaryAbsenceOccurrence.Companion.RELEASE_AT
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.TemporaryAbsenceOccurrence.Companion.RETURN_BY
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.removeNullChar
import java.time.LocalDate

data class TapOccurrenceSearchRequest(
  @NotBlank
  val prisonCode: String,
  val fromDate: LocalDate,
  val toDate: LocalDate,
  val query: String? = null,
  override val page: Int = 1,
  override val size: Int = 10,
  override val sort: String = DATE_RANGE,
) : PagedRequest {
  override fun validSortFields(): Set<String> = setOf(DATE_RANGE)

  private fun sortByDate(direction: Direction) = by(direction, RELEASE_AT, RETURN_BY, PERSON_IDENTIFIER)

  @JsonIgnore
  val queryString: String? = query?.removeNullChar()?.takeIf { it.isNotBlank() }

  override fun buildSort(field: String, direction: Direction): Sort = when (field) {
    DATE_RANGE -> sortByDate(direction)
    else -> throw IllegalArgumentException("Unrecognised sort field")
  }

  companion object {
    const val DATE_RANGE = "date"
  }
}
