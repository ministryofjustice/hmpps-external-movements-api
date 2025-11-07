package uk.gov.justice.digital.hmpps.externalmovementsapi.model.paged

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.validation.constraints.NotBlank
import org.springframework.data.domain.Sort
import org.springframework.data.domain.Sort.Direction
import org.springframework.data.domain.Sort.by
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.authorisation.TemporaryAbsenceAuthorisation.Companion.FROM_DATE
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.authorisation.TemporaryAbsenceAuthorisation.Companion.PERSON_IDENTIFIER
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.authorisation.TemporaryAbsenceAuthorisation.Companion.TO_DATE
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.TapAuthorisationStatus
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.removeNullChar
import java.time.LocalDate

data class TapAuthorisationSearchRequest(
  @NotBlank
  val prisonCode: String,
  val fromDate: LocalDate,
  val toDate: LocalDate,
  val status: Set<TapAuthorisationStatus.Code> = emptySet(),
  val query: String? = null,
  override val page: Int = 1,
  override val size: Int = 10,
  override val sort: String = DATE_RANGE,
) : PagedRequest {
  override fun validSortFields(): Set<String> = setOf(DATE_RANGE, STATUS)

  private fun sortByDate(direction: Direction) = by(direction, FROM_DATE, TO_DATE, PERSON_IDENTIFIER)

  @JsonIgnore
  val queryString: String? = query?.removeNullChar()?.takeIf { it.isNotBlank() }

  override fun buildSort(field: String, direction: Direction): Sort = when (field) {
    DATE_RANGE -> sortByDate(direction)
    STATUS -> by(direction, "status.description")
    else -> throw IllegalArgumentException("Unrecognised sort field")
  }

  companion object {
    const val DATE_RANGE = "date"
    const val STATUS = "status"
  }
}
