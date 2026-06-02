package uk.gov.justice.digital.hmpps.externalmovementsapi.tap.model.actions.authorisation

import uk.gov.justice.digital.hmpps.externalmovementsapi.events.DomainEvent
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.TemporaryAbsenceAuthorisationDateRangeChanged
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.DateRange
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.StartAndEnd
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.ValidDateRange
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.ValidStartAndEnd
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.domain.authorisation.TemporaryAbsenceAuthorisation
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
