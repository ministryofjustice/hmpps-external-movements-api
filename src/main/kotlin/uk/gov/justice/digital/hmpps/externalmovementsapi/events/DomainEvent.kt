package uk.gov.justice.digital.hmpps.externalmovementsapi.events

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.JsonTypeInfo
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.DataSource
import java.time.ZonedDateTime

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "eventType")
@JsonSubTypes(
  value = [
    Type(value = TemporaryAbsenceAuthorised::class, name = TemporaryAbsenceAuthorised.EVENT_TYPE),
    Type(value = TemporaryAbsenceRescheduled::class, name = TemporaryAbsenceRescheduled.EVENT_TYPE),
    Type(value = TemporaryAbsenceCancelled::class, name = TemporaryAbsenceCancelled.EVENT_TYPE),
    Type(value = TemporaryAbsenceExpired::class, name = TemporaryAbsenceExpired.EVENT_TYPE),
    Type(value = TemporaryAbsenceOverdue::class, name = TemporaryAbsenceOverdue.EVENT_TYPE),
  ],
)
sealed interface DomainEvent<T : AdditionalInformation> {
  val occurredAt: ZonedDateTime
    get() = ZonedDateTime.now()
  val eventType: String
  val description: String
  val additionalInformation: T
  val personReference: PersonReference
  val detailUrl: String?
    get() = null
  val version: Int
    get() = 1
}

data class PersonReference(val identifiers: List<Identifier> = listOf()) {
  operator fun get(key: String): String? = identifiers.find { it.type == key }?.value

  companion object {
    const val NOMS_NUMBER_TYPE = "NOMS"
    fun withIdentifier(personIdentifier: String) = PersonReference(listOf(Identifier(NOMS_NUMBER_TYPE, personIdentifier)))
  }

  data class Identifier(val type: String, val value: String)
}

sealed interface AdditionalInformation {
  val source: DataSource
}
