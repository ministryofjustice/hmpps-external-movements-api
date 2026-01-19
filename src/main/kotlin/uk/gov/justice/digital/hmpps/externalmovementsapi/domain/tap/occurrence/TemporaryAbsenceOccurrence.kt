package uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.occurrence

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
import org.hibernate.annotations.Formula
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.envers.Audited
import org.hibernate.envers.NotAudited
import org.hibernate.envers.RelationTargetAuditMode.NOT_AUDITED
import org.hibernate.type.SqlTypes
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.ExternalMovementContext
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.set
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.IdGenerator.newUuid
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.Identifiable
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.ReasonPath
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.interceptor.DomainEventProducer
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.person.PersonSummary
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceData
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.CategorisedAbsenceReason
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.authorisation.TemporaryAbsenceAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.movement.TemporaryAbsenceMovement
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.movement.TemporaryAbsenceMovement.Companion.formattedReason
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.AccompaniedBy
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.AuthorisationStatus
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.OccurrenceStatus
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.OccurrenceStatus.Code.CANCELLED
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.OccurrenceStatus.Code.COMPLETED
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.OccurrenceStatus.Code.EXPIRED
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.OccurrenceStatus.Code.IN_PROGRESS
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.OccurrenceStatus.Code.OVERDUE
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.OccurrenceStatus.Code.PENDING
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.OccurrenceStatus.Code.SCHEDULED
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.Transport
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.absencereason.AbsenceReason
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.absencereason.AbsenceReasonCategory
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.absencereason.AbsenceSubType
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.absencereason.AbsenceType
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.DomainEvent
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.TemporaryAbsenceScheduled
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.occurrence.CancelOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.occurrence.ChangeOccurrenceAccompaniment
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.occurrence.ChangeOccurrenceComments
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.occurrence.ChangeOccurrenceContactInformation
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.occurrence.ChangeOccurrenceLocation
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.occurrence.ChangeOccurrenceTransport
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.occurrence.CommenceOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.occurrence.CompleteOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.occurrence.DenyOccurrence
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
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty0
import kotlin.reflect.KProperty1

@Audited
@Entity
@Table(schema = "tap", name = "occurrence")
class TemporaryAbsenceOccurrence(
  @Audited
  @ManyToOne
  @JoinColumn(name = "authorisation_id", updatable = false)
  val authorisation: TemporaryAbsenceAuthorisation,
  absenceType: AbsenceType?,
  absenceSubType: AbsenceSubType?,
  absenceReasonCategory: AbsenceReasonCategory?,
  absenceReason: AbsenceReason?,
  start: LocalDateTime,
  end: LocalDateTime,
  accompaniedBy: AccompaniedBy,
  transport: Transport,
  location: Location,
  contactInformation: String?,
  comments: String?,
  reasonPath: ReasonPath,
  scheduleReference: JsonNode?,
  legacyId: Long?,
  @Id
  @Column(name = "id", nullable = false, updatable = false)
  override val id: UUID = newUuid(),
) : Identifiable,
  CategorisedAbsenceReason,
  DomainEventProducer {
  @Audited(targetAuditMode = NOT_AUDITED)
  @ManyToOne(optional = false)
  @JoinColumn(name = "status_id", nullable = false)
  lateinit var status: OccurrenceStatus
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

  @Audited(targetAuditMode = NOT_AUDITED)
  @ManyToOne
  @JoinColumn(name = "person_identifier")
  var person: PersonSummary = authorisation.person
    private set

  @Size(max = 6)
  @NotNull
  @Column(name = "prison_code", nullable = false, length = 6)
  var prisonCode: String = authorisation.prisonCode
    private set

  @NotNull
  @Column(name = "start", nullable = false)
  var start: LocalDateTime = start
    private set

  @NotNull
  @Column(name = "end", nullable = false)
  var end: LocalDateTime = end
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

  // Use only for sorting
  @NotAudited
  @Formula(value = "(coalesce({alias}.location->>'description'),'') || coalesce(({alias}.location->>'address'),'') || coalesce(({alias}.location->>'postcode'),'')")
  private val locationDescription: String? = null

  @Column(name = "contact_information")
  var contactInformation: String? = contactInformation
    private set

  @Column(name = "comments")
  var comments: String? = comments
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
  override var version: Int? = null
    private set

  @OneToMany(mappedBy = "occurrence", cascade = [CascadeType.PERSIST, CascadeType.MERGE])
  private val movements: MutableList<TemporaryAbsenceMovement> = mutableListOf()
  fun movements(): List<TemporaryAbsenceMovement> = unmodifiableList(movements)

  fun addMovement(movement: TemporaryAbsenceMovement, statusProvider: (String) -> OccurrenceStatus) = apply {
    movements.add(movement)
    movement.occurrence = this
    ExternalMovementContext.get().takeIf { !it.migratingData }?.also {
      it.copy(reason = formattedReason(movement)).set()
    }
    calculateStatus(statusProvider)
  }

  fun removeMovement(movement: TemporaryAbsenceMovement, statusProvider: (String) -> OccurrenceStatus) = apply {
    movements.remove(movement)
    calculateStatus(statusProvider)
  }

  override fun initialEvent(): DomainEvent<*>? = when (status.code) {
    SCHEDULED.name -> TemporaryAbsenceScheduled(person.identifier, id)
    else -> null
  }

  @Transient
  private var appliedActions: List<OccurrenceAction> = listOf()

  @PostLoad
  private fun load() {
    appliedActions = listOf()
  }

  override fun domainEvents(): Set<DomainEvent<*>> {
    val events = appliedActions.mapNotNull { it.domainEvent(this) }.toSet()
    appliedActions = emptyList()
    return events
  }

  fun applyAbsenceCategorisation(
    action: RecategoriseOccurrence,
    rdSupplier: (KClass<out ReferenceData>, String) -> ReferenceData,
  ) {
    if (action.changes(this)) {
      reasonPath = action.reasonPath
      absenceType = action.absenceTypeCode?.let { rdSupplier(AbsenceType::class, it) as AbsenceType }
      absenceSubType = action.absenceSubTypeCode?.let { rdSupplier(AbsenceSubType::class, it) as AbsenceSubType }
      absenceReasonCategory =
        action.absenceReasonCategoryCode?.let { rdSupplier(AbsenceReasonCategory::class, it) as AbsenceReasonCategory }
      absenceReason =
        action.absenceReasonCode?.let { rdSupplier(AbsenceReason::class, it) as AbsenceReason }
      appliedActions += action
    }
  }

  fun reschedule(action: RescheduleOccurrence) {
    val rel = action.start.ifChanges(::start)
    val ret = action.end.ifChanges(::end)
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
    rdSupplier: (KClass<out ReferenceData>, String) -> ReferenceData,
  ) {
    if (accompaniedBy.code != action.accompaniedByCode) {
      accompaniedBy = rdSupplier(AccompaniedBy::class, action.accompaniedByCode) as AccompaniedBy
      appliedActions += action
    }
  }

  fun applyTransport(
    action: ChangeOccurrenceTransport,
    rdSupplier: (KClass<out ReferenceData>, String) -> ReferenceData,
  ) {
    if (transport.code != action.transportCode) {
      transport = rdSupplier(Transport::class, action.transportCode) as Transport
      appliedActions += action
    }
  }

  fun applyContactInformation(action: ChangeOccurrenceContactInformation) {
    if (contactInformation?.startsWith(action.information) != true) {
      contactInformation = action.information
      appliedActions += action
    }
  }

  fun applyComments(action: ChangeOccurrenceComments) {
    if (action.changes(comments)) {
      comments = action.comments
      appliedActions += action
    }
  }

  fun cancel(action: CancelOccurrence, rdSupplier: (KClass<out ReferenceData>, String) -> ReferenceData) {
    if (!::status.isInitialized || status.code != CANCELLED.name) {
      status = rdSupplier(OccurrenceStatus::class, CANCELLED.name) as OccurrenceStatus
      appliedActions += action
    }
  }

  fun calculateStatus(statusProvider: (String) -> OccurrenceStatus) = apply {
    status =
      statusProvider(
        listOfNotNull(
          movementStatus(),
          expiredStatus(),
          isCancelledStatus(),
          authorisationStatus(),
        ).first().name,
      )
  }

  private fun movementStatus(): OccurrenceStatus.Code? = movements.takeIf { it.isNotEmpty() }
    ?.map { it.direction }?.let {
      if (it.contains(TemporaryAbsenceMovement.Direction.IN)) {
        complete()
      } else if (end.isAfter(now())) {
        commence()
      } else {
        if (::status.isInitialized && status.code != OVERDUE.name) {
          appliedActions += MarkOccurrenceOverdue()
        }
        OVERDUE
      }
    }

  private fun expiredStatus(): OccurrenceStatus.Code? = if (isExpired() || (authorisation.canExpire() && shouldExpire())) {
    if (!isExpired()) {
      appliedActions += ExpireOccurrence()
    }
    EXPIRED
  } else {
    null
  }

  private fun isCancelledStatus(): OccurrenceStatus.Code? = if (::status.isInitialized && status.code == CANCELLED.name) {
    CANCELLED
  } else {
    null
  }

  private fun authorisationStatus(): OccurrenceStatus.Code = if (authorisation.status.code == AuthorisationStatus.Code.APPROVED.name) {
    approvedAuthorisationStatuses()
  } else {
    val status = OccurrenceStatus.Code.valueOf(authorisation.status.code)
    val action = when (status) {
      OccurrenceStatus.Code.DENIED -> DenyOccurrence()
      else -> null
    }
    action?.also {
      if (::status.isInitialized && this.status.code != status.name) {
        appliedActions += action
      }
    }
    status
  }

  private fun approvedAuthorisationStatuses(): OccurrenceStatus.Code {
    if (::status.isInitialized && status.code == PENDING.name) {
      appliedActions += ScheduleOccurrence()
    }
    return SCHEDULED
  }

  private fun isExpired() = ::status.isInitialized && status.code == EXPIRED.name

  private fun TemporaryAbsenceAuthorisation.canExpire(): Boolean = status.code in listOf(
    AuthorisationStatus.Code.PENDING.name,
    AuthorisationStatus.Code.APPROVED.name,
    AuthorisationStatus.Code.EXPIRED.name,
  )

  private fun shouldExpire(): Boolean = (::status.isInitialized.not() || status.code in listOf(PENDING.name, SCHEDULED.name, EXPIRED.name)) &&
    end.isBefore(now())

  private fun commence(): OccurrenceStatus.Code {
    if (::status.isInitialized && status.code != IN_PROGRESS.name) {
      appliedActions += CommenceOccurrence()
    }
    return IN_PROGRESS
  }

  private fun complete(): OccurrenceStatus.Code {
    if (::status.isInitialized && status.code != COMPLETED.name) {
      appliedActions += CompleteOccurrence()
    }
    return COMPLETED
  }

  companion object {
    val PRISON_CODE = TemporaryAbsenceOccurrence::prisonCode.name
    val PERSON = TemporaryAbsenceOccurrence::person.name
    val AUTHORISATION = TemporaryAbsenceOccurrence::authorisation.name
    val START = TemporaryAbsenceOccurrence::start.name
    val END = TemporaryAbsenceOccurrence::end.name
    val STATUS = TemporaryAbsenceOccurrence::status.name
    val ABSENCE_TYPE = TemporaryAbsenceOccurrence::absenceType.name
    val ABSENCE_SUB_TYPE = TemporaryAbsenceOccurrence::absenceSubType.name
    val ABSENCE_REASON_CATEGORY = TemporaryAbsenceOccurrence::absenceReasonCategory.name
    val ABSENCE_REASON = TemporaryAbsenceOccurrence::absenceReason.name
    val ACCOMPANIED_BY = TemporaryAbsenceOccurrence::accompaniedBy.name
    val TRANSPORT = TemporaryAbsenceOccurrence::transport.name
    val LOCATION = TemporaryAbsenceOccurrence::location.name

    fun changeableProperties(): Set<KProperty1<TemporaryAbsenceOccurrence, Any?>> = setOf(
      TemporaryAbsenceOccurrence::start,
      TemporaryAbsenceOccurrence::end,
      TemporaryAbsenceOccurrence::accompaniedBy,
      TemporaryAbsenceOccurrence::transport,
      TemporaryAbsenceOccurrence::contactInformation,
      TemporaryAbsenceOccurrence::comments,
      TemporaryAbsenceOccurrence::location,
      TemporaryAbsenceOccurrence::absenceType,
      TemporaryAbsenceOccurrence::absenceSubType,
      TemporaryAbsenceOccurrence::absenceReasonCategory,
      TemporaryAbsenceOccurrence::absenceReason,
      TemporaryAbsenceOccurrence::status,
    )
  }
}

private fun LocalDateTime?.ifChanges(property: KMutableProperty0<LocalDateTime>): Boolean = if (this == null || truncatedTo(SECONDS).isEqual(property.get().truncatedTo(SECONDS))) {
  false
} else {
  property.set(this)
  true
}
