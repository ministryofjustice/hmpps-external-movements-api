package uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.occurrence

import com.fasterxml.jackson.databind.JsonNode
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.ManyToOne
import jakarta.persistence.PostLoad
import jakarta.persistence.Table
import jakarta.persistence.Transient
import jakarta.persistence.Version
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.envers.Audited
import org.hibernate.envers.NotAudited
import org.hibernate.envers.RelationTargetAuditMode.NOT_AUDITED
import org.hibernate.type.SqlTypes
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.IdGenerator.newUuid
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.Identifiable
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.TemporaryAbsenceAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.interceptor.Actionable
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.interceptor.DomainEventProducer
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.AccompaniedBy
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceData
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.TapAuthorisationStatus
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.TapOccurrenceStatus
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.Transport
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.DomainEvent
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.TemporaryAbsenceScheduled
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.occurrence.OccurrenceAction
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.occurrence.RescheduleOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.location.Location
import uk.gov.justice.digital.hmpps.externalmovementsapi.sync.write.ScheduledTemporaryAbsenceRequest
import java.time.LocalDateTime
import java.util.UUID

@Audited
@Entity
@Table(name = "temporary_absence_occurrence")
class TemporaryAbsenceOccurrence(
  @Audited
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "authorisation_id", updatable = false)
  val authorisation: TemporaryAbsenceAuthorisation,
  releaseAt: LocalDateTime,
  returnBy: LocalDateTime,
  accompaniedBy: AccompaniedBy,
  transport: Transport,
  location: Location,
  contactInformation: String?,
  notes: String?,
  addedAt: LocalDateTime,
  addedBy: String,
  cancelledAt: LocalDateTime?,
  cancelledBy: String?,
  scheduleReference: JsonNode?,
  legacyId: Long?,
  @Id
  @Column(name = "id", nullable = false, updatable = false)
  override val id: UUID = newUuid(),
) : Identifiable,
  Actionable,
  DomainEventProducer {
  @Size(max = 10)
  @NotNull
  @Column(name = "person_identifier", nullable = false, length = 10)
  var personIdentifier: String = authorisation.personIdentifier
    private set

  @NotAudited
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinTable(
    name = "temporary_absence_occurrence_status",
    joinColumns = [JoinColumn(name = "occurrence_id", referencedColumnName = "id")],
    inverseJoinColumns = [JoinColumn(name = "status_id", referencedColumnName = "id")],
  )
  var status: TapOccurrenceStatus? = null
    private set

  @NotNull
  @Column(name = "release_at", nullable = false)
  var releaseAt: LocalDateTime = releaseAt
    private set

  @NotNull
  @Column(name = "return_by", nullable = false)
  var returnBy: LocalDateTime = returnBy
    private set

  @Audited(targetAuditMode = NOT_AUDITED)
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "accompanied_by_id")
  var accompaniedBy: AccompaniedBy = accompaniedBy
    private set

  @Audited(targetAuditMode = NOT_AUDITED)
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "transport_id")
  var transport: Transport = transport
    private set

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "location")
  var location: Location = location
    private set

  @Column(name = "contact_information")
  var contactInformation: String? = contactInformation
    private set

  @Column(name = "notes")
  var notes: String? = notes
    private set

  @NotNull
  @Column(name = "added_at", nullable = false)
  var addedAt: LocalDateTime = addedAt
    private set

  @NotNull
  @Column(name = "added_by", nullable = false)
  var addedBy: String = addedBy
    private set

  @Column(name = "cancelled_at")
  var cancelledAt: LocalDateTime? = cancelledAt
    private set

  @Column(name = "cancelled_by", nullable = false)
  var cancelledBy: String? = cancelledBy
    private set

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "schedule_reference")
  var scheduleReference: JsonNode? = scheduleReference
    private set

  @Column(name = "legacy_id")
  var legacyId: Long? = legacyId
    private set

  @Version
  var version: Int? = null
    private set

  fun update(
    request: ScheduledTemporaryAbsenceRequest,
    rdProvider: (ReferenceDataDomain.Code, String) -> ReferenceData,
  ) = apply {
    releaseAt = request.startTime
    returnBy = request.returnTime
    contactInformation = request.contactPersonName
    location = request.location.asLocation()
    accompaniedBy = rdProvider(ReferenceDataDomain.Code.ACCOMPANIED_BY, request.escortOrDefault()) as AccompaniedBy
    transport = rdProvider(ReferenceDataDomain.Code.TRANSPORT, request.transportTypeOrDefault()) as Transport
    notes = request.comment
    addedAt = request.audit.createDatetime
    addedBy = request.audit.createUsername
    cancelledAt = request.cancelledAt
    cancelledBy = request.cancelledBy
    legacyId = request.eventId
  }

  override fun initialEvent(): DomainEvent<*>? = if (authorisation.status.code == TapAuthorisationStatus.Code.APPROVED.name) {
    TemporaryAbsenceScheduled(personIdentifier, id)
  } else {
    null
  }

  @Transient
  private var appliedActions: List<OccurrenceAction> = listOf()

  @PostLoad
  private fun load() {
    appliedActions = listOf()
  }

  override fun actions(): List<TapOccurrenceAction> = appliedActions?.map {
    TapOccurrenceAction(this, it.type, it.reason)
  } ?: emptyList()

  override fun domainEvents(): Set<DomainEvent<*>> = appliedActions?.map { it.domainEvent(this) }?.toSet() ?: emptySet()

  fun reschedule(action: RescheduleOccurrence) {
    action.releaseAt?.also { releaseAt = it }
    action.returnBy?.also { returnBy = it }
    appliedActions += action
  }

  companion object {
    val AUTHORISATION = TemporaryAbsenceOccurrence::authorisation.name
    val PERSON_IDENTIFIER = TemporaryAbsenceOccurrence::personIdentifier.name
    val RELEASE_AT = TemporaryAbsenceOccurrence::releaseAt.name
    val RETURN_BY = TemporaryAbsenceOccurrence::returnBy.name
  }
}
