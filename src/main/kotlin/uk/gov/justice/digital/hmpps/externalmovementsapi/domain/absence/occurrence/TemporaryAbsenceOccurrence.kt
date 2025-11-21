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
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.envers.Audited
import org.hibernate.envers.RelationTargetAuditMode.NOT_AUDITED
import org.hibernate.type.SqlTypes
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.IdGenerator.newUuid
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.Identifiable
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.ReasonPath
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.AbsenceCategorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.authorisation.TemporaryAbsenceAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.movement.TemporaryAbsenceMovement
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.interceptor.DomainEventProducer
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.AbsenceReason
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.AbsenceReasonCategory
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.AbsenceSubType
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.AbsenceType
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.AccompaniedBy
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceData
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_REASON
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_REASON_CATEGORY
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_SUB_TYPE
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_TYPE
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ACCOMPANIED_BY
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.TAP_OCCURRENCE_STATUS
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.TRANSPORT
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.TapAuthorisationStatus.Code.APPROVED
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.TapOccurrenceStatus
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.TapOccurrenceStatus.Code.CANCELLED
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.TapOccurrenceStatus.Code.COMPLETED
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.TapOccurrenceStatus.Code.EXPIRED
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.TapOccurrenceStatus.Code.IN_PROGRESS
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.TapOccurrenceStatus.Code.OVERDUE
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.TapOccurrenceStatus.Code.PENDING
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.TapOccurrenceStatus.Code.SCHEDULED
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.Transport
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.DomainEvent
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.TemporaryAbsenceScheduled
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.occurrence.AmendOccurrenceNotes
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.occurrence.CancelOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.occurrence.ChangeOccurrenceAccompaniment
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.occurrence.ChangeOccurrenceContactInformation
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.occurrence.ChangeOccurrenceLocation
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.occurrence.ChangeOccurrenceTransport
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.occurrence.ExpireOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.occurrence.MarkOccurrenceOverdue
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.occurrence.OccurrenceAction
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.occurrence.RecategoriseOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.occurrence.RescheduleOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.occurrence.ScheduleOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.location.Location
import java.time.LocalDateTime
import java.time.LocalDateTime.now
import java.time.temporal.ChronoUnit.SECONDS
import java.util.Collections.unmodifiableList
import java.util.UUID
import kotlin.reflect.KMutableProperty0
import kotlin.reflect.KProperty1

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
  reasonPath: ReasonPath,
  scheduleReference: JsonNode?,
  legacyId: Long?,
  @Id
  @Column(name = "id", nullable = false, updatable = false)
  override val id: UUID = newUuid(),
) : Identifiable,
  AbsenceCategorisation,
  DomainEventProducer {
  @Audited(targetAuditMode = NOT_AUDITED)
  @ManyToOne(optional = false)
  @JoinColumn(name = "status_id", nullable = false)
  lateinit var status: TapOccurrenceStatus
    private set

  @Audited(targetAuditMode = NOT_AUDITED)
  @ManyToOne
  @JoinColumn(name = "absence_type_id")
  override var absenceType: AbsenceType? = absenceType
    private set

  @Audited(targetAuditMode = NOT_AUDITED)
  @ManyToOne
  @JoinColumn(name = "absence_sub_type_id")
  override var absenceSubType: AbsenceSubType? = absenceSubType
    private set

  @Audited(targetAuditMode = NOT_AUDITED)
  @ManyToOne
  @JoinColumn(name = "absence_reason_category_id")
  override var absenceReasonCategory: AbsenceReasonCategory? = absenceReasonCategory
    private set

  @Audited(targetAuditMode = NOT_AUDITED)
  @ManyToOne
  @JoinColumn(name = "absence_reason_id")
  override var absenceReason: AbsenceReason? = absenceReason
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

  override fun initialEvent(): DomainEvent<*>? = when (status.code) {
    SCHEDULED.name -> TemporaryAbsenceScheduled(authorisation.personIdentifier, id)
    else -> null
  }

  @Transient
  private var appliedActions: List<OccurrenceAction> = listOf()

  @PostLoad
  private fun load() {
    appliedActions = listOf()
  }

  override fun domainEvents(): Set<DomainEvent<*>> = appliedActions.mapNotNull { it.domainEvent(this) }.toSet()

  fun applyAbsenceCategorisation(
    action: RecategoriseOccurrence,
    rdSupplier: (ReferenceDataDomain.Code, String) -> ReferenceData,
  ) {
    if (action.changes(this)) {
      absenceType = action.absenceTypeCode?.let { rdSupplier(ABSENCE_TYPE, it) as AbsenceType }
    }
    absenceSubType = action.absenceSubTypeCode?.let { rdSupplier(ABSENCE_SUB_TYPE, it) as AbsenceSubType }
    absenceReasonCategory =
      action.absenceReasonCategoryCode?.let { rdSupplier(ABSENCE_REASON_CATEGORY, it) as AbsenceReasonCategory }
    absenceReason = action.absenceReasonCode?.let { rdSupplier(ABSENCE_REASON, it) as AbsenceReason }
    reasonPath = action.reasonPath
    appliedActions += action
  }

  fun reschedule(action: RescheduleOccurrence) {
    val rel = action.releaseAt.ifChanges(::releaseAt)
    val ret = action.returnBy.ifChanges(::returnBy)
    if (rel || ret) {
      appliedActions += action
    }
  }

  fun applyLocation(action: ChangeOccurrenceLocation) {
    if (location != action.location) {
      location = action.location
      appliedActions += action
    }
  }

  fun applyAccompaniment(
    action: ChangeOccurrenceAccompaniment,
    rdSupplier: (ReferenceDataDomain.Code, String) -> ReferenceData,
  ) {
    if (accompaniedBy.code != action.accompaniedByCode) {
      accompaniedBy = rdSupplier(ACCOMPANIED_BY, action.accompaniedByCode) as AccompaniedBy
      appliedActions += action
    }
  }

  fun applyTransport(
    action: ChangeOccurrenceTransport,
    rdSupplier: (ReferenceDataDomain.Code, String) -> ReferenceData,
  ) {
    if (transport.code != action.transportCode) {
      transport = rdSupplier(TRANSPORT, action.transportCode) as Transport
      appliedActions += action
    }
  }

  fun applyContactInformation(action: ChangeOccurrenceContactInformation) {
    if (contactInformation?.startsWith(action.information) != true) {
      contactInformation = action.information
      appliedActions += action
    }
  }

  fun amendNotes(action: AmendOccurrenceNotes) {
    if (action.changes(notes)) {
      notes = action.notes
      appliedActions += action
    }
  }

  fun cancel(action: CancelOccurrence, statusProvider: (ReferenceDataDomain.Code, String) -> ReferenceData) {
    if (!::status.isInitialized || status.code != CANCELLED.name) {
      status = statusProvider(TAP_OCCURRENCE_STATUS, CANCELLED.name) as TapOccurrenceStatus
      appliedActions += action
    }
  }

  fun calculateStatus(statusProvider: (String) -> TapOccurrenceStatus) = apply {
    status =
      statusProvider(listOfNotNull(movementStatus(), isCancelledStatus(), authorisationStatus()).first().name)
  }

  private fun isCancelledStatus(): TapOccurrenceStatus.Code? = if (::status.isInitialized && status.code == CANCELLED.name) {
    CANCELLED
  } else {
    null
  }

  private fun movementStatus(): TapOccurrenceStatus.Code? = movements.takeIf { it.isNotEmpty() }
    ?.map { it.direction }?.let {
      if (it.contains(TemporaryAbsenceMovement.Direction.IN)) {
        COMPLETED
      } else if (returnBy.isAfter(now())) {
        IN_PROGRESS
      } else {
        if (::status.isInitialized && status.code != OVERDUE.name) {
          appliedActions += MarkOccurrenceOverdue()
        }
        OVERDUE
      }
    }

  private fun authorisationStatus(): TapOccurrenceStatus.Code = if (authorisation.status.code == APPROVED.name) {
    if (movements.isEmpty() && returnBy.isBefore(now())) {
      if (::status.isInitialized && status.code == SCHEDULED.name) {
        appliedActions += ExpireOccurrence()
      }
      EXPIRED
    } else {
      if (::status.isInitialized && status.code == PENDING.name) {
        appliedActions += ScheduleOccurrence()
      }
      SCHEDULED
    }
  } else {
    TapOccurrenceStatus.Code.valueOf(authorisation.status.code)
  }

  companion object {
    val AUTHORISATION = TemporaryAbsenceOccurrence::authorisation.name
    val RELEASE_AT = TemporaryAbsenceOccurrence::releaseAt.name
    val RETURN_BY = TemporaryAbsenceOccurrence::returnBy.name
    val STATUS = TemporaryAbsenceOccurrence::status.name

    fun changeableProperties(): Set<KProperty1<TemporaryAbsenceOccurrence, Any?>> = setOf(
      TemporaryAbsenceOccurrence::releaseAt,
      TemporaryAbsenceOccurrence::returnBy,
      TemporaryAbsenceOccurrence::accompaniedBy,
      TemporaryAbsenceOccurrence::transport,
      TemporaryAbsenceOccurrence::contactInformation,
      TemporaryAbsenceOccurrence::notes,
      TemporaryAbsenceOccurrence::location,
      TemporaryAbsenceOccurrence::absenceType,
      TemporaryAbsenceOccurrence::absenceSubType,
      TemporaryAbsenceOccurrence::absenceReasonCategory,
      TemporaryAbsenceOccurrence::absenceReason,
    )
  }
}

private fun LocalDateTime?.ifChanges(property: KMutableProperty0<LocalDateTime>): Boolean = if (this == null || truncatedTo(SECONDS).isEqual(property.get().truncatedTo(SECONDS))) {
  false
} else {
  property.set(this)
  true
}
