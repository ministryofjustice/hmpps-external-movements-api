package uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.authorisation

import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.authorisation.TemporaryAbsenceAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.DomainEvent
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.TemporaryAbsenceAuthorisationDateRangeChanged
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.DateRange
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.ValidDateRange
import java.time.LocalDate

@ValidDateRange
data class ChangeAuthorisationDateRange(
  override val fromDate: LocalDate,
  override val toDate: LocalDate,
  override val reason: String? = null,
) : AuthorisationAction,
  DateRange {
  override fun domainEvent(taa: TemporaryAbsenceAuthorisation): DomainEvent<*> = TemporaryAbsenceAuthorisationDateRangeChanged(taa.person.identifier, taa.id)
}
