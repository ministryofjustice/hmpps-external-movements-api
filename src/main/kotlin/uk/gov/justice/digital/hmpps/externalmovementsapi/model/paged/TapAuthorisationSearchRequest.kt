package uk.gov.justice.digital.hmpps.externalmovementsapi.model.paged

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.validation.constraints.NotBlank
import org.springframework.data.domain.Sort
import org.springframework.data.domain.Sort.Direction
import org.springframework.data.domain.Sort.by
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.authorisation.TemporaryAbsenceAuthorisation.Companion.ABSENCE_REASON
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.authorisation.TemporaryAbsenceAuthorisation.Companion.ABSENCE_TYPE
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.authorisation.TemporaryAbsenceAuthorisation.Companion.END
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.authorisation.TemporaryAbsenceAuthorisation.Companion.PERSON
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.authorisation.TemporaryAbsenceAuthorisation.Companion.REPEAT
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.authorisation.TemporaryAbsenceAuthorisation.Companion.START
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.authorisation.TemporaryAbsenceAuthorisation.Companion.STATUS
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.person.PersonSummary.Companion.FIRST_NAME
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.person.PersonSummary.Companion.IDENTIFIER
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.person.PersonSummary.Companion.LAST_NAME
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceData.Companion.SEQUENCE_NUMBER
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.TapAuthorisationStatus
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.removeNullChar
import java.time.LocalDate

data class TapAuthorisationSearchRequest(
  @NotBlank
  val prisonCode: String,
  val start: LocalDate?,
  val end: LocalDate?,
  val status: Set<TapAuthorisationStatus.Code> = emptySet(),
  val query: String? = null,
  override val page: Int = 1,
  override val size: Int = 10,
  override val sort: String = START,
) : PagedRequest {
  override fun validSortFields(): Set<String> = setOf(START, END, STATUS, ABSENCE_TYPE, ABSENCE_REASON, REPEAT, FIRST_NAME, LAST_NAME)

  private fun sortByDate(direction: Direction, first: String, second: String) = by(direction, first, second).and(sortByPersonName())

  private fun sortByPersonName(
    direction: Direction = Direction.ASC,
    first: String = PERSON_LAST_NAME,
    second: String = PERSON_FIRST_NAME,
  ) = by(direction, first, second, "${PERSON}_$IDENTIFIER")

  @JsonIgnore
  val queryString: String? = query?.removeNullChar()?.takeIf { it.isNotBlank() }

  override fun buildSort(field: String, direction: Direction): Sort = when (field) {
    LAST_NAME -> sortByPersonName(direction)
    FIRST_NAME -> sortByPersonName(direction, PERSON_FIRST_NAME, PERSON_LAST_NAME)
    START -> sortByDate(direction, START, END)
    END -> sortByDate(direction, END, START)
    STATUS -> by(direction, "${STATUS}_${SEQUENCE_NUMBER}").and(sortByPersonName())
    ABSENCE_TYPE, ABSENCE_REASON -> by(direction, "${field}_description").and(sortByPersonName())
    else -> by(direction, field).and(sortByPersonName())
  }

  companion object {
    private val PERSON_LAST_NAME = "${PERSON}_${LAST_NAME}"
    private val PERSON_FIRST_NAME = "${PERSON}_${FIRST_NAME}"
  }
}
