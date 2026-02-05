package uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.movement

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
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
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.ExternalMovementContext
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.set
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.IdGenerator.newUuid
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.Identifiable
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.event.producer.DomainEventProducer
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.event.producer.DomainEventPublication
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.event.producer.publication
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.person.PersonSummary
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceData
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.occurrence.TemporaryAbsenceOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.AccompaniedBy
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.OccurrenceStatus
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.absencereason.AbsenceReason
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.TapMovementOccurrenceChanged
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.TemporaryAbsenceCompleted
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.TemporaryAbsenceStarted
import uk.gov.justice.digital.hmpps.externalmovementsapi.exception.NotFoundException
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.movement.ChangeMovementAccompaniment
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.movement.ChangeMovementComments
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.movement.ChangeMovementDirection
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.movement.ChangeMovementLocation
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.movement.ChangeMovementOccurredAt
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.movement.ChangeMovementOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.movement.ChangeMovementReason
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.movement.MovementAction
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.location.Location
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

@Audited
@Entity
@Table(schema = "tap", name = "movement")
class TemporaryAbsenceMovement(
  person: PersonSummary,
  occurrence: TemporaryAbsenceOccurrence?,
  occurredAt: LocalDateTime,
  direction: Direction,
  absenceReason: AbsenceReason,
  accompaniedBy: AccompaniedBy,
  accompaniedByComments: String?,
  comments: String?,
  location: Location,
  prisonCode: String,
  legacyId: String?,
  @Id
  @Column(name = "id", nullable = false, updatable = false)
  override val id: UUID = newUuid(),
) : Identifiable,
  DomainEventProducer {
  @Audited(targetAuditMode = NOT_AUDITED)
  @ManyToOne
  @JoinColumn(name = "person_identifier")
  var person: PersonSummary = person
    private set

  @ManyToOne
  @JoinColumn(name = "occurrence_id")
  var occurrence: TemporaryAbsenceOccurrence? = occurrence
    internal set

  @NotNull
  @Column(name = "occurred_at", nullable = false)
  var occurredAt: LocalDateTime = occurredAt
    private set

  @NotNull
  @Enumerated(EnumType.STRING)
  @Column(name = "direction", nullable = false, length = 3)
  var direction: Direction = direction
    private set

  @Audited(targetAuditMode = NOT_AUDITED)
  @NotNull
  @ManyToOne(optional = false)
  @JoinColumn(name = "absence_reason_id", nullable = false)
  var absenceReason: AbsenceReason = absenceReason
    private set

  @Audited(targetAuditMode = NOT_AUDITED)
  @NotNull
  @ManyToOne(optional = false)
  @JoinColumn(name = "accompanied_by_id", nullable = false)
  var accompaniedBy: AccompaniedBy = accompaniedBy
    private set

  @Column(name = "accompanied_by_comments", length = Integer.MAX_VALUE)
  var accompaniedByComments: String? = accompaniedByComments
    private set

  @Column(name = "comments", length = Integer.MAX_VALUE)
  var comments: String? = comments
    private set

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "location")
  var location: Location = location
    private set

  @Size(max = 6)
  @NotNull
  @Column(name = "prison_code", nullable = false, length = 6)
  var prisonCode: String = prisonCode
    private set

  @Column(name = "legacy_id")
  var legacyId: String? = legacyId
    private set

  @Version
  override var version: Int? = null
    private set

  @Transient
  private var appliedActions: List<MovementAction> = listOf()

  @PostLoad
  private fun load() {
    appliedActions = listOf()
  }

  override fun initialEvents(): Set<DomainEventPublication> = setOf(
    when (direction) {
      Direction.OUT -> TemporaryAbsenceStarted(person.identifier, id, occurrence?.id)
      Direction.IN -> TemporaryAbsenceCompleted(person.identifier, id, occurrence?.id)
    }.publication(id),
  )

  override fun domainEvents(): Set<DomainEventPublication> = appliedActions.mapNotNull { action ->
    action.domainEvent(this)?.publication(id) { it.eventType !in EXCLUDE_FROM_PUBLISH }
  }.toSet()

  enum class Direction {
    IN,
    OUT,
  }

  fun moveTo(person: PersonSummary) = apply {
    this.person = person
  }

  fun switchSchedule(
    action: ChangeMovementOccurrence,
    rdSupplier: (KClass<out ReferenceData>, String) -> ReferenceData,
    occurrenceSupplier: (UUID) -> TemporaryAbsenceOccurrence,
  ) = apply {
    if (this.occurrence?.id != action.occurrenceId) {
      val oldOccurrence = this.occurrence
      oldOccurrence?.removeMovement(this) { rdSupplier(OccurrenceStatus::class, it) as OccurrenceStatus }
      val newOccurrence = action.occurrenceId?.let { occurrenceSupplier(it) }
      newOccurrence?.addMovement(this) { rdSupplier(OccurrenceStatus::class, it) as OccurrenceStatus }
      ExternalMovementContext.get().copy(reason = "Recorded movement temporary absence occurrence changed").set()
      appliedActions += action
    }
  }

  fun applyDirection(action: ChangeMovementDirection) = apply {
    if (direction != action.direction) {
      direction = action.direction
      appliedActions += action
    }
  }

  fun applyLocation(action: ChangeMovementLocation) = apply {
    if (location != action.location) {
      location = action.location
      appliedActions += action
    }
  }

  fun applyAccompaniedBy(
    action: ChangeMovementAccompaniment,
    rdSupplier: (KClass<out ReferenceData>, String) -> ReferenceData,
  ) = apply {
    var changed = false
    if (action changes accompaniedByComments) {
      accompaniedByComments = action.comments
      changed = true
    }
    if (accompaniedBy.code != action.accompaniedByCode) {
      accompaniedBy = rdSupplier(AccompaniedBy::class, action.accompaniedByCode) as AccompaniedBy
      changed = true
    }
    if (changed) {
      appliedActions += action
    }
  }

  fun applyComments(action: ChangeMovementComments) = apply {
    if (action changes comments) {
      comments = action.comments
      appliedActions += action
    }
  }

  fun applyOccurredAt(action: ChangeMovementOccurredAt) = apply {
    if (action changes occurredAt) {
      occurredAt = action.occurredAt
      appliedActions += action
    }
  }

  fun applyReason(
    action: ChangeMovementReason,
    rdSupplier: (KClass<out ReferenceData>, String) -> ReferenceData,
  ) = apply {
    if (absenceReason.code != action.absenceReasonCode) {
      absenceReason = rdSupplier(AbsenceReason::class, action.absenceReasonCode) as AbsenceReason
      appliedActions += action
    }
  }

  companion object {
    val EXCLUDE_FROM_PUBLISH: Set<String> = setOf(
      TapMovementOccurrenceChanged.EVENT_TYPE,
    )

    val formattedReason: (TemporaryAbsenceMovement) -> String = {
      val date = DateTimeFormatter.ofPattern("EEEE dd MMMM yyyy")
      val time = DateTimeFormatter.ofPattern("HH:mm")
      val format = date.format(it.occurredAt) + " at " + time.format(it.occurredAt)
      when (it.direction) {
        Direction.IN -> "Recorded as having returned in to the prison on $format."
        Direction.OUT -> "Recorded as having gone out of the prison on $format."
      }
    }

    fun changeableProperties(): Set<KProperty1<TemporaryAbsenceMovement, Any?>> = setOf(
      TemporaryAbsenceMovement::direction,
      TemporaryAbsenceMovement::occurredAt,
      TemporaryAbsenceMovement::absenceReason,
      TemporaryAbsenceMovement::location,
      TemporaryAbsenceMovement::accompaniedBy,
      TemporaryAbsenceMovement::accompaniedByComments,
      TemporaryAbsenceMovement::comments,
    )
  }
}

interface TemporaryAbsenceMovementRepository :
  JpaRepository<TemporaryAbsenceMovement, UUID>,
  JpaSpecificationExecutor<TemporaryAbsenceMovement> {
  fun findByLegacyId(legacyId: String): TemporaryAbsenceMovement?

  fun countByOccurrenceId(occurrenceId: UUID): Int
  fun findAllByPersonIdentifier(personIdentifier: String): List<TemporaryAbsenceMovement>

  @Query(
    """
      select 
        coalesce(sum(case when tam.occurrence is not null and tam.direction = 'OUT' then 1 else 0 end),0) as schOut,
        coalesce(sum(case when tam.occurrence is not null and tam.direction = 'IN' then 1 else 0 end),0) as schIn,
        coalesce(sum(case when tam.occurrence is null and tam.direction = 'OUT' then 1 else 0 end),0) as adocOut,
        coalesce(sum(case when tam.occurrence is null and tam.direction = 'IN' then 1 else 0 end),0) as adhocIn
      from TemporaryAbsenceMovement tam
      where tam.person.identifier = :personIdentifier
    """,
  )
  fun summaryForPerson(personIdentifier: String): PersonMovementSummary

  @Modifying
  @Query("delete from TemporaryAbsenceMovement tam where tam.person.identifier = :personIdentifier")
  fun deleteByPersonIdentifier(personIdentifier: String)
}

interface PersonMovementSummary {
  val schOut: Int
  val schIn: Int
  val adocOut: Int
  val adhocIn: Int
}

fun TemporaryAbsenceMovementRepository.getMovement(id: UUID) = findByIdOrNull(id) ?: throw NotFoundException("Temporary absence movement not found")
