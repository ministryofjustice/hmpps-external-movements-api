package uk.gov.justice.digital.hmpps.externalmovementsapi.model.paged

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.validation.constraints.NotBlank
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.removeNullChar

data class LocationSearchRequest(
  @NotBlank
  val query: String,
  override val page: Int = 1,
  override val size: Int = 50,
  override val sort: String = NAME,
) : PagedRequest {
  override fun validSortFields(): Set<String> = setOf(NAME)

  @JsonIgnore
  val queryString: String = query.removeNullChar().also { require(it.isNotBlank()) }

  companion object {
    const val NAME = "name"
  }
}
