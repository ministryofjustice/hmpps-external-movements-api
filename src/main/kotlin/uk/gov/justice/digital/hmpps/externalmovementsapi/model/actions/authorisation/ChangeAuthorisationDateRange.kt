package uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.authorisation

import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.authorisation.TemporaryAbsenceAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.DomainEvent
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.TemporaryAbsenceAuthorisationDateRangeChanged
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.StartAndEnd
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.ValidStartAndEnd
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.DateRange
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.ValidDateRange
import java.time.LocalDate

@ValidDateRange
@ValidStartAndEnd
data class ChangeAuthorisationDateRange(
  override val start: LocalDate,
  override val end: LocalDate,
  override val reason: String? = null,
) : AuthorisationAction,
  DateRange,
  StartAndEnd<LocalDate> {
  override fun domainEvent(taa: TemporaryAbsenceAuthorisation): DomainEvent<*> = TemporaryAbsenceAuthorisationDateRangeChanged(taa.person.identifier, taa.id)
}
