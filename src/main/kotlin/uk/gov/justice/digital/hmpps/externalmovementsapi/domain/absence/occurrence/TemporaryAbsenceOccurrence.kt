package uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.occurrence

import com.fasterxml.jackson.databind.JsonNode
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.PostLoad
import jakarta.persistence.Table
import jakarta.persistence.Transient
import jakarta.persistence.Version
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.envers.Audited
import org.hibernate.envers.RelationTargetAuditMode.NOT_AUDITED
import org.hibernate.type.SqlTypes
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.IdGenerator.newUuid
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.Identifiable
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.ReasonPath
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.authorisation.TemporaryAbsenceAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.movement.TemporaryAbsenceMovement
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.interceptor.DomainEventProducer
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.AbsenceReason
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.AbsenceReasonCategory
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.AbsenceSubType
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.AbsenceType
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.AccompaniedBy
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_REASON
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_REASON_CATEGORY
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_SUB_TYPE
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_TYPE
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.TAP_OCCURRENCE_STATUS
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataPaths
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.TapAuthorisationStatus
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.TapOccurrenceStatus
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.Transport
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.DomainEvent
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.TemporaryAbsenceScheduled
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.occurrence.CancelOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.occurrence.ExpireOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.occurrence.MarkOccurrenceOverdue
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.occurrence.OccurrenceAction
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.occurrence.RescheduleOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.location.Location
import uk.gov.justice.digital.hmpps.externalmovementsapi.sync.write.TapOccurrence
import java.time.LocalDateTime
import java.time.LocalDateTime.now
import java.time.temporal.ChronoUnit
import java.util.Collections.unmodifiableList
import java.util.UUID

@Audited
@Entity
@Table(name = "temporary_absence_occurrence")
class TemporaryAbsenceOccurrence(
  @Audited
  @ManyToOne
  @JoinColumn(name = "authorisation_id", updatable = false)
  val authorisation: TemporaryAbsenceAuthorisation,
  absenceType: AbsenceType?,
  absenceSubType: AbsenceSubType?,
  absenceReasonCategory: AbsenceReasonCategory?,
  absenceReason: AbsenceReason?,
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
  reasonPath: ReasonPath,
  scheduleReference: JsonNode?,
  legacyId: Long?,
  @Id
  @Column(name = "id", nullable = false, updatable = false)
  override val id: UUID = newUuid(),
) : Identifiable,
  DomainEventProducer {
  @Size(max = 10)
  @NotNull
  @Column(name = "person_identifier", nullable = false, length = 10)
  var personIdentifier: String = authorisation.personIdentifier
    private set

  @Audited(targetAuditMode = NOT_AUDITED)
  @ManyToOne(optional = false)
  @JoinColumn(name = "status_id", nullable = false)
  lateinit var status: TapOccurrenceStatus
    private set

  @Audited(targetAuditMode = NOT_AUDITED)
  @ManyToOne
  @JoinColumn(name = "absence_type_id")
  var absenceType: AbsenceType? = absenceType
    private set

  @Audited(targetAuditMode = NOT_AUDITED)
  @ManyToOne
  @JoinColumn(name = "absence_sub_type_id")
  var absenceSubType: AbsenceSubType? = absenceSubType
    private set

  @Audited(targetAuditMode = NOT_AUDITED)
  @ManyToOne
  @JoinColumn(name = "absence_reason_category_id")
  var absenceReasonCategory: AbsenceReasonCategory? = absenceReasonCategory
    private set

  @Audited(targetAuditMode = NOT_AUDITED)
  @ManyToOne
  @JoinColumn(name = "absence_reason_id")
  var absenceReason: AbsenceReason? = absenceReason
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
  @ManyToOne(optional = false)
  @JoinColumn(name = "accompanied_by_id", nullable = false)
  var accompaniedBy: AccompaniedBy = accompaniedBy
    private set

  @Audited(targetAuditMode = NOT_AUDITED)
  @ManyToOne(optional = false)
  @JoinColumn(name = "transport_id", nullable = false)
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
  @Column(name = "reason_path")
  var reasonPath: ReasonPath = reasonPath
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

  @OneToMany(mappedBy = "occurrence", cascade = [CascadeType.PERSIST, CascadeType.MERGE])
  private val movements: MutableList<TemporaryAbsenceMovement> = mutableListOf()
  fun movements(): List<TemporaryAbsenceMovement> = unmodifiableList(movements)

  fun addMovement(movement: TemporaryAbsenceMovement, statusProvider: (String) -> TapOccurrenceStatus) = apply {
    movements.add(movement)
    movement.occurrence = this
    calculateStatus(statusProvider)
  }

  fun removeMovement(movement: TemporaryAbsenceMovement, statusProvider: (String) -> TapOccurrenceStatus) = apply {
    movements.remove(movement)
    calculateStatus(statusProvider)
  }

  fun update(
    request: TapOccurrence,
    rdPaths: ReferenceDataPaths,
  ) = apply {
    reasonPath = rdPaths.reasonPath()
    absenceType = request.absenceTypeCode?.let { rdPaths.getReferenceData(ABSENCE_TYPE, it) as AbsenceType }
    absenceSubType =
      request.absenceSubTypeCode?.let { rdPaths.getReferenceData(ABSENCE_SUB_TYPE, it) as AbsenceSubType }
    absenceReasonCategory = reasonPath.path.singleOrNull { it.domain == ABSENCE_REASON_CATEGORY }?.let {
      rdPaths.getReferenceData(it.domain, it.code)
    } as? AbsenceReasonCategory
    absenceReason = rdPaths.getReferenceData(ABSENCE_REASON, request.absenceReasonCode) as AbsenceReason
    if (!request.releaseAt.truncatedTo(ChronoUnit.SECONDS).isEqual(releaseAt.truncatedTo(ChronoUnit.SECONDS)) ||
      !request.returnBy.truncatedTo(ChronoUnit.SECONDS).isEqual(returnBy.truncatedTo(ChronoUnit.SECONDS))
    ) {
      releaseAt = request.releaseAt
      returnBy = request.returnBy
      appliedActions += RescheduleOccurrence(releaseAt, returnBy, null)
    }
    contactInformation = null
    location = request.location
    accompaniedBy =
      rdPaths.getReferenceData(ReferenceDataDomain.Code.ACCOMPANIED_BY, request.accompaniedByCode) as AccompaniedBy
    transport = rdPaths.getReferenceData(ReferenceDataDomain.Code.TRANSPORT, request.transportCode) as Transport
    notes = request.notes
    addedAt = request.created.at
    addedBy = request.created.by
    if (request.statusCode in listOf(
        TapOccurrenceStatus.Code.CANCELLED.name,
        TapOccurrenceStatus.Code.DENIED.name,
      ) &&
      request.updated != null
    ) {
      cancelledAt = request.updated.at
      cancelledBy = request.updated.by
      appliedActions += CancelOccurrence()
    }
    legacyId = request.legacyId
    calculateStatus {
      rdPaths.getReferenceData(TAP_OCCURRENCE_STATUS, it) as TapOccurrenceStatus
    }
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

  override fun domainEvents(): Set<DomainEvent<*>> = appliedActions.map { it.domainEvent(this) }.toSet()

  fun reschedule(action: RescheduleOccurrence) {
    action.releaseAt?.also { releaseAt = it }
    action.returnBy?.also { returnBy = it }
    appliedActions += action
  }

  fun calculateStatus(statusProvider: (String) -> TapOccurrenceStatus) = apply {
    status =
      statusProvider(listOfNotNull(movementStatus(), isCancelledStatus(), authorisationStatus()).first().name)
  }

  private fun isCancelledStatus(): TapOccurrenceStatus.Code? = cancelledAt?.let { TapOccurrenceStatus.Code.CANCELLED }

  private fun movementStatus(): TapOccurrenceStatus.Code? = movements.takeIf { it.isNotEmpty() }
    ?.map { it.direction }?.let {
      if (it.contains(TemporaryAbsenceMovement.Direction.IN)) {
        TapOccurrenceStatus.Code.COMPLETED
      } else if (returnBy.isAfter(now())) {
        TapOccurrenceStatus.Code.IN_PROGRESS
      } else {
        appliedActions += MarkOccurrenceOverdue()
        TapOccurrenceStatus.Code.OVERDUE
      }
    }

  private fun authorisationStatus(): TapOccurrenceStatus.Code = if (authorisation.status.code == TapAuthorisationStatus.Code.APPROVED.name) {
    if (movements.isEmpty() && returnBy.isBefore(now())) {
      appliedActions += ExpireOccurrence()
      TapOccurrenceStatus.Code.EXPIRED
    } else {
      TapOccurrenceStatus.Code.SCHEDULED
    }
  } else {
    TapOccurrenceStatus.Code.valueOf(authorisation.status.code)
  }

  companion object {
    val AUTHORISATION = TemporaryAbsenceOccurrence::authorisation.name
    val PERSON_IDENTIFIER = TemporaryAbsenceOccurrence::personIdentifier.name
    val RELEASE_AT = TemporaryAbsenceOccurrence::releaseAt.name
    val RETURN_BY = TemporaryAbsenceOccurrence::returnBy.name
  }
}
