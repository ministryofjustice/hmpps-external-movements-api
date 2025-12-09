package uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.occurrence

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.JsonTypeInfo
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.occurrence.TemporaryAbsenceOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.DomainEvent
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.Action

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
  value = [
    Type(value = RecategoriseOccurrence::class, name = "RecategoriseOccurrence"),
    Type(value = RescheduleOccurrence::class, name = "RescheduleOccurrence"),
    Type(value = CancelOccurrence::class, name = "CancelOccurrence"),
    Type(value = ChangeOccurrenceAccompaniment::class, name = "ChangeOccurrenceAccompaniment"),
    Type(value = ChangeOccurrenceTransport::class, name = "ChangeOccurrenceTransport"),
    Type(value = ChangeOccurrenceLocation::class, name = "ChangeOccurrenceLocation"),
    Type(value = AmendOccurrenceNotes::class, name = "AmendOccurrenceNotes"),
  ],
)
sealed interface OccurrenceAction : Action {
  fun domainEvent(tao: TemporaryAbsenceOccurrence): DomainEvent<*>? = null
}
