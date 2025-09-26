package uk.gov.justice.digital.hmpps.externalmovementsapi.model.paged

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.validation.constraints.NotBlank
import org.springframework.data.domain.Sort
import org.springframework.data.domain.Sort.Direction
import org.springframework.data.domain.Sort.by
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.TapAuthorisationStatus
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

  private fun sortByDate(direction: Direction) = by(direction, "fromDate", "toDate", "personIdentifier")

  @JsonIgnore
  val queryString: String? = query?.replace("\u0000", "")?.trim()?.takeIf { it.isNotBlank() }

  override fun buildSort(field: String, direction: Direction): Sort = when (field) {
    DATE_RANGE -> sortByDate(direction)
    STATUS -> by(direction, "statusDescription")
    else -> throw IllegalArgumentException("Unrecognised sort field")
  }

  companion object {
    const val DATE_RANGE = "date"
    const val STATUS = "status"
  }
}
