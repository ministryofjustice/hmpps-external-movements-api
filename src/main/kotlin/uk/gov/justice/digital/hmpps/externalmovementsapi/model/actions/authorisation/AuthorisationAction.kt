package uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.authorisation

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.JsonTypeInfo
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.authorisation.TemporaryAbsenceAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.DomainEvent
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.Action

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
  value = [
    Type(value = ApproveAuthorisation::class, name = "ApproveAuthorisation"),
    Type(value = DenyAuthorisation::class, name = "DenyAuthorisation"),
    Type(value = CancelAuthorisation::class, name = "CancelAuthorisation"),
    Type(value = ChangeAuthorisationAccompaniment::class, name = "ChangeAuthorisationAccompaniment"),
    Type(value = ChangeAuthorisationComments::class, name = "ChangeAuthorisationComments"),
    Type(value = ChangeAuthorisationDateRange::class, name = "ChangeAuthorisationDateRange"),
    Type(value = RecategoriseAuthorisation::class, name = "RecategoriseAuthorisation"),
  ],
)
sealed interface AuthorisationAction : Action {
  fun domainEvent(taa: TemporaryAbsenceAuthorisation): DomainEvent<*>? = null
}
