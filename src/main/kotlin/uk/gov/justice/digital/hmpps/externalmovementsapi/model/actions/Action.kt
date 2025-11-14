package uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.JsonTypeInfo
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.occurrence.CancelOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.occurrence.RescheduleOccurrence

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
  value = [
    Type(value = RescheduleOccurrence::class, name = "RescheduleOccurrence"),
    Type(value = CancelOccurrence::class, name = "CancelOccurrence"),
  ],
)
interface Action {
  val reason: String?
}
