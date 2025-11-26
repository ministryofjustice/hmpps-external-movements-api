package uk.gov.justice.digital.hmpps.externalmovementsapi.model.paged

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.validation.constraints.NotBlank
import org.springframework.data.domain.Sort
import org.springframework.data.domain.Sort.Direction
import org.springframework.data.domain.Sort.by
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.authorisation.TemporaryAbsenceAuthorisation.Companion.ABSENCE_REASON
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.authorisation.TemporaryAbsenceAuthorisation.Companion.ABSENCE_TYPE
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.authorisation.TemporaryAbsenceAuthorisation.Companion.FROM_DATE
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.authorisation.TemporaryAbsenceAuthorisation.Companion.PERSON
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.authorisation.TemporaryAbsenceAuthorisation.Companion.REPEAT
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.authorisation.TemporaryAbsenceAuthorisation.Companion.STATUS
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.authorisation.TemporaryAbsenceAuthorisation.Companion.TO_DATE
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
  val fromDate: LocalDate?,
  val toDate: LocalDate?,
  val status: Set<TapAuthorisationStatus.Code> = emptySet(),
  val query: String? = null,
  override val page: Int = 1,
  override val size: Int = 10,
  override val sort: String = FROM_DATE,
) : PagedRequest {
  override fun validSortFields(): Set<String> = setOf(FROM_DATE, TO_DATE, STATUS, ABSENCE_TYPE, ABSENCE_REASON, REPEAT, FIRST_NAME, LAST_NAME)

  private fun sortByDate(direction: Direction, first: String, second: String) = by(direction, first, second).and(sortByPersonName())

  private fun sortByPersonName(
    direction: Direction = Direction.ASC,
    first: String = "${PERSON}_${LAST_NAME}",
    second: String = "${PERSON}_${FIRST_NAME}",
  ) = by(direction, first, second, "${PERSON}_$IDENTIFIER")

  @JsonIgnore
  val queryString: String? = query?.removeNullChar()?.takeIf { it.isNotBlank() }

  override fun buildSort(field: String, direction: Direction): Sort = when (field) {
    FIRST_NAME -> sortByPersonName(direction, "${PERSON}_${FIRST_NAME}", "${PERSON}_${LAST_NAME}")
    LAST_NAME -> sortByPersonName(direction)
    FROM_DATE -> sortByDate(direction, FROM_DATE, TO_DATE)
    TO_DATE -> sortByDate(direction, TO_DATE, FROM_DATE)
    STATUS -> by(direction, "${STATUS}_${SEQUENCE_NUMBER}")
    ABSENCE_TYPE, ABSENCE_REASON -> by(direction, "${field}_description").and(sortByPersonName())

    else -> throw IllegalArgumentException("Unrecognised sort field")
  }
}
