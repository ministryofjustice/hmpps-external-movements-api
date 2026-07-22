package uk.gov.justice.digital.hmpps.externalmovementsapi.tap.model

import jakarta.validation.Valid
import jakarta.validation.constraints.FutureOrPresent
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.StartAndEnd
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.ValidStartAndEnd
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.location.Location
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.domain.authorisation.TemporaryAbsenceAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.domain.occurrence.TemporaryAbsenceOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.domain.referencedata.AuthorisationStatus
import java.time.LocalDateTime

@ValidStartAndEnd
data class CreateOccurrenceRequest(
  @FutureOrPresent(message = "Absence cannot be scheduled in the past.")
  override val start: LocalDateTime,
  override val end: LocalDateTime,
  @Valid
  val location: Location,
  val comments: String?,
) : StartAndEnd<LocalDateTime>

fun CreateOccurrenceRequest.asOccurrence(
  authorisation: TemporaryAbsenceAuthorisation,
): TemporaryAbsenceOccurrence = TemporaryAbsenceOccurrence(
  authorisation,
  absenceType = authorisation.absenceType,
  absenceSubType = authorisation.absenceSubType,
  absenceReasonCategory = authorisation.absenceReasonCategory,
  absenceReason = authorisation.absenceReason,
  start = start,
  end = end,
  accompaniedBy = authorisation.accompaniedBy,
  transport = authorisation.transport,
  location = location.let {
    if (it.address?.isEmpty() == true) {
      it.copy(address = null)
    } else {
      it
    }
  },
  contactInformation = null,
  comments = comments ?: authorisation.comments,
  reasonPath = authorisation.reasonPath,
  legacyId = null,
  dpsOnly = authorisation.status.code != AuthorisationStatus.Code.APPROVED.name,
)
