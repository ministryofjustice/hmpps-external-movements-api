package uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.movement

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
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
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.IdGenerator.newUuid
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.Identifiable
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceData
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.movement.TemporaryAbsenceMovement.Direction.valueOf
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.occurrence.TemporaryAbsenceOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.AccompaniedBy
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.absencereason.AbsenceReason
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.location.Location
import uk.gov.justice.digital.hmpps.externalmovementsapi.sync.write.TapMovement
import java.time.LocalDateTime
import java.util.UUID
import kotlin.reflect.KClass

@Audited
@Entity
@Table(schema = "tap", name = "movement")
class TemporaryAbsenceMovement(
  personIdentifier: String,
  occurrence: TemporaryAbsenceOccurrence?,
  occurredAt: LocalDateTime,
  direction: Direction,
  absenceReason: AbsenceReason,
  accompaniedBy: AccompaniedBy,
  accompaniedByComments: String?,
  comments: String?,
  location: Location,
  recordedByPrisonCode: String,
  legacyId: String?,
  @Id
  @Column(name = "id", nullable = false, updatable = false)
  override val id: UUID = newUuid(),
) : Identifiable {
  @Size(max = 10)
  @NotNull
  @Column(name = "person_identifier", nullable = false, length = 10)
  var personIdentifier: String = personIdentifier.uppercase()
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
  @Column(name = "recorded_by_prison_code", nullable = false, length = 6)
  var recordedByPrisonCode: String = recordedByPrisonCode
    private set

  @Column(name = "legacy_id")
  var legacyId: String? = legacyId
    private set

  @Version
  override var version: Int? = null
    private set

  enum class Direction {
    IN,
    OUT,
  }

  fun moveTo(personIdentifier: String) = apply {
    this.personIdentifier = personIdentifier
  }

  fun update(
    personIdentifier: String,
    occurrence: TemporaryAbsenceOccurrence?,
    request: TapMovement,
    rdProvider: (KClass<out ReferenceData>, String) -> ReferenceData,
  ) = apply {
    this.personIdentifier = personIdentifier
    this.occurrence = occurrence
    occurredAt = request.occurredAt
    direction = valueOf(request.direction.name)
    absenceReason = rdProvider(AbsenceReason::class, request.absenceReasonCode) as AbsenceReason
    accompaniedBy = rdProvider(AccompaniedBy::class, request.accompaniedByCode) as AccompaniedBy
    accompaniedByComments = request.accompaniedByComments
    comments = request.comments
    recordedByPrisonCode = request.created.prisonCode
    location = request.location
    legacyId = request.legacyId
  }
}

interface TemporaryAbsenceMovementRepository :
  JpaRepository<TemporaryAbsenceMovement, UUID>,
  JpaSpecificationExecutor<TemporaryAbsenceMovement> {
  fun findByLegacyId(legacyId: String): TemporaryAbsenceMovement?

  fun findByOccurrenceId(occurrenceId: UUID): List<TemporaryAbsenceMovement>
  fun findByOccurrenceIdIn(ids: Set<UUID>): List<TemporaryAbsenceMovement>
  fun findAllByPersonIdentifier(personIdentifier: String): List<TemporaryAbsenceMovement>

  @Query(
    """
      select 
        coalesce(sum(case when tam.occurrence is not null and tam.direction = 'OUT' then 1 else 0 end),0) as schOut,
        coalesce(sum(case when tam.occurrence is not null and tam.direction = 'IN' then 1 else 0 end),0) as schIn,
        coalesce(sum(case when tam.occurrence is null and tam.direction = 'OUT' then 1 else 0 end),0) as adocOut,
        coalesce(sum(case when tam.occurrence is null and tam.direction = 'IN' then 1 else 0 end),0) as adhocIn
      from TemporaryAbsenceMovement tam
      where tam.personIdentifier = :personIdentifier
    """,
  )
  fun summaryForPerson(personIdentifier: String): PersonMovementSummary

  @Modifying
  @Query("delete from TemporaryAbsenceMovement tam where tam.personIdentifier = :personIdentifier")
  fun deleteByPersonIdentifier(personIdentifier: String)
}

interface PersonMovementSummary {
  val schOut: Int
  val schIn: Int
  val adocOut: Int
  val adhocIn: Int
}
