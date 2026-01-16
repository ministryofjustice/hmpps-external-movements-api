package uk.gov.justice.digital.hmpps.externalmovementsapi.events

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.JsonTypeInfo
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.DataSource
import java.time.ZonedDateTime
import java.util.UUID

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "eventType")
@JsonSubTypes(
  value = [
    Type(value = TemporaryAbsenceAuthorisationPending::class, name = TemporaryAbsenceAuthorisationPending.EVENT_TYPE),
    Type(value = TemporaryAbsenceAuthorisationApproved::class, name = TemporaryAbsenceAuthorisationApproved.EVENT_TYPE),
    Type(value = TemporaryAbsenceAuthorisationDeferred::class, name = TemporaryAbsenceAuthorisationDeferred.EVENT_TYPE),
    Type(value = TemporaryAbsenceAuthorisationDenied::class, name = TemporaryAbsenceAuthorisationDenied.EVENT_TYPE),
    Type(value = TemporaryAbsenceAuthorisationCancelled::class, name = TemporaryAbsenceAuthorisationCancelled.EVENT_TYPE),
    Type(value = TemporaryAbsenceAuthorisationExpired::class, name = TemporaryAbsenceAuthorisationExpired.EVENT_TYPE),
    Type(value = TemporaryAbsenceAuthorisationDateRangeChanged::class, name = TemporaryAbsenceAuthorisationDateRangeChanged.EVENT_TYPE),
    Type(value = TemporaryAbsenceAuthorisationAccompanimentChanged::class, name = TemporaryAbsenceAuthorisationAccompanimentChanged.EVENT_TYPE),
    Type(value = TemporaryAbsenceAuthorisationTransportChanged::class, name = TemporaryAbsenceAuthorisationTransportChanged.EVENT_TYPE),
    Type(value = TemporaryAbsenceAuthorisationCommentsChanged::class, name = TemporaryAbsenceAuthorisationCommentsChanged.EVENT_TYPE),
    Type(value = TemporaryAbsenceAuthorisationRecategorised::class, name = TemporaryAbsenceAuthorisationRecategorised.EVENT_TYPE),

    Type(value = TemporaryAbsenceScheduled::class, name = TemporaryAbsenceScheduled.EVENT_TYPE),
    Type(value = TemporaryAbsenceRescheduled::class, name = TemporaryAbsenceRescheduled.EVENT_TYPE),
    Type(value = TemporaryAbsenceCancelled::class, name = TemporaryAbsenceCancelled.EVENT_TYPE),
    Type(value = TemporaryAbsenceDenied::class, name = TemporaryAbsenceDenied.EVENT_TYPE),
    Type(value = TemporaryAbsenceExpired::class, name = TemporaryAbsenceExpired.EVENT_TYPE),
    Type(value = TemporaryAbsenceOverdue::class, name = TemporaryAbsenceOverdue.EVENT_TYPE),
    Type(value = TemporaryAbsenceAccompanimentChanged::class, name = TemporaryAbsenceAccompanimentChanged.EVENT_TYPE),
    Type(value = TemporaryAbsenceTransportChanged::class, name = TemporaryAbsenceTransportChanged.EVENT_TYPE),
    Type(value = TemporaryAbsenceCommentsChanged::class, name = TemporaryAbsenceCommentsChanged.EVENT_TYPE),
    Type(value = TemporaryAbsenceRecategorised::class, name = TemporaryAbsenceRecategorised.EVENT_TYPE),
    Type(value = TemporaryAbsenceRelocated::class, name = TemporaryAbsenceRelocated.EVENT_TYPE),
    Type(value = TemporaryAbsenceStarted::class, name = TemporaryAbsenceStarted.EVENT_TYPE),
    Type(value = TemporaryAbsenceCompleted::class, name = TemporaryAbsenceCompleted.EVENT_TYPE),

    Type(value = PrisonerUpdated::class, name = PrisonerUpdated.EVENT_TYPE),
    Type(value = PrisonerMerged::class, name = PrisonerMerged.EVENT_TYPE),

    Type(value = TemporaryAbsenceAuthorisationMigrated::class, name = TemporaryAbsenceAuthorisationMigrated.EVENT_TYPE),
    Type(value = TemporaryAbsenceMigrated::class, name = TemporaryAbsenceMigrated.EVENT_TYPE),
    Type(value = ExternalMovementMigrated::class, name = ExternalMovementMigrated.EVENT_TYPE),
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
  fun getPersonIdentifier(): String = checkNotNull(personReference.findPersonIdentifier())
}

data class PersonReference(val identifiers: List<Identifier> = listOf()) {
  operator fun get(key: String): String? = identifiers.find { it.type == key }?.value
  fun findPersonIdentifier() = get(NOMS_NUMBER_TYPE)

  companion object {
    const val NOMS_NUMBER_TYPE = "NOMS"
    fun withIdentifier(personIdentifier: String) = PersonReference(listOf(Identifier(NOMS_NUMBER_TYPE, personIdentifier)))
  }

  data class Identifier(val type: String, val value: String)
}

sealed interface AdditionalInformation {
  val source: DataSource
}

sealed interface IdInformation {
  val id: UUID
}
