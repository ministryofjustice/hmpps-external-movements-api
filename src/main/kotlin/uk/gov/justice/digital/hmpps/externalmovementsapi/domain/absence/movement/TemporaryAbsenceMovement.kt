package uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.movement

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
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.IdGenerator.newUuid
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.Identifiable
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.movement.TemporaryAbsenceMovement.Direction.valueOf
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.occurrence.TemporaryAbsenceOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.AbsenceReason
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.AccompaniedBy
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceData
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.location.Location
import uk.gov.justice.digital.hmpps.externalmovementsapi.sync.write.TapMovement
import java.time.LocalDateTime
import java.util.UUID

@Audited
@Entity
@Table(name = "temporary_absence_movement")
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

  fun update(
    personIdentifier: String,
    occurrence: TemporaryAbsenceOccurrence?,
    request: TapMovement,
    rdProvider: (ReferenceDataDomain.Code, String) -> ReferenceData,
  ) = apply {
    this.personIdentifier = personIdentifier
    this.occurrence = occurrence
    occurredAt = request.occurredAt
    direction = valueOf(request.direction.name)
    absenceReason = rdProvider(ReferenceDataDomain.Code.ABSENCE_REASON, request.absenceReasonCode) as AbsenceReason
    accompaniedBy = rdProvider(ReferenceDataDomain.Code.ACCOMPANIED_BY, request.accompaniedByCode) as AccompaniedBy
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

  @Modifying
  fun deleteByPersonIdentifier(personIdentifier: String)
}
