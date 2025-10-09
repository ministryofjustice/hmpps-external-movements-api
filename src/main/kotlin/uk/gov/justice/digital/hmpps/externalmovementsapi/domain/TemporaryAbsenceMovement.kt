package uk.gov.justice.digital.hmpps.externalmovementsapi.domain

import jakarta.persistence.Column
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.Version
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.IdGenerator.newUuid
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.TemporaryAbsenceMovement.Direction.valueOf
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.AbsenceReason
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.AccompaniedBy
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.LocationType
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceData
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain
import uk.gov.justice.digital.hmpps.externalmovementsapi.sync.TapLocation
import uk.gov.justice.digital.hmpps.externalmovementsapi.sync.TapMovementRequest
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "temporary_absence_movement")
class TemporaryAbsenceMovement(
  personIdentifier: String,
  occurrence: TemporaryAbsenceOccurrence?,
  occurredAt: LocalDateTime,
  direction: Direction,
  absenceReason: AbsenceReason,
  accompaniedBy: AccompaniedBy,
  accompaniedByNotes: String?,
  notes: String?,
  location: Location,
  recordedAt: LocalDateTime,
  recordedBy: String,
  recordedByPrisonCode: String,
  legacyId: Long?,
  @Id
  @Column(name = "id", nullable = false)
  val id: UUID = newUuid(),
) {
  @Size(max = 10)
  @NotNull
  @Column(name = "person_identifier", nullable = false, length = 10)
  var personIdentifier: String = personIdentifier
    private set

  @ManyToOne
  @JoinColumn(name = "occurrence_id")
  var occurrence: TemporaryAbsenceOccurrence? = occurrence
    private set

  @NotNull
  @Column(name = "occurred_at", nullable = false)
  var occurredAt: LocalDateTime = occurredAt
    private set

  @NotNull
  @Enumerated(EnumType.STRING)
  @Column(name = "direction", nullable = false, length = 3)
  var direction: Direction = direction
    private set

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "absence_reason_id", nullable = false)
  var absenceReason: AbsenceReason = absenceReason
    private set

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "accompanied_by_id", nullable = false)
  var accompaniedBy: AccompaniedBy = accompaniedBy
    private set

  @Column(name = "accompanied_by_notes", length = Integer.MAX_VALUE)
  var accompaniedByNotes: String? = accompaniedByNotes
    private set

  @Column(name = "notes", length = Integer.MAX_VALUE)
  var notes: String? = notes
    private set

  @Embedded
  var location: Location = location
    private set

  @NotNull
  @Column(name = "recorded_at", nullable = false)
  var recordedAt: LocalDateTime = recordedAt
    private set

  @Size(max = 64)
  @NotNull
  @Column(name = "recorded_by", nullable = false, length = 64)
  var recordedBy: String = recordedBy
    private set

  @Size(max = 6)
  @NotNull
  @Column(name = "recorded_by_prison_code", nullable = false, length = 6)
  var recordedByPrisonCode: String = recordedByPrisonCode
    private set

  @Column(name = "legacy_id")
  var legacyId: Long? = legacyId
    private set

  @Version
  var version: Int? = null
    private set

  enum class Direction {
    IN,
    OUT,
  }

  fun update(
    personIdentifier: String,
    occurrence: TemporaryAbsenceOccurrence?,
    request: TapMovementRequest,
    rdProvider: (ReferenceDataDomain.Code, String) -> ReferenceData,
  ) = apply {
    this.personIdentifier = personIdentifier
    this.occurrence = occurrence
    occurredAt = request.movementDateTime
    direction = valueOf(request.direction.name)
    absenceReason = rdProvider(ReferenceDataDomain.Code.ABSENCE_REASON, request.movementReason) as AbsenceReason
    accompaniedBy = rdProvider(ReferenceDataDomain.Code.ACCOMPANIED_BY, request.escortOrDefault()) as AccompaniedBy
    accompaniedByNotes = request.escortText
    notes = request.commentText
    recordedAt = request.audit.createDatetime
    recordedBy = request.audit.createUsername
    recordedByPrisonCode = request.prisonCodeOrDefault()
    location = request.location.embedded(rdProvider)
    legacyId = request.legacyId
  }
}

fun TapLocation.embedded(
  rdProvider: (ReferenceDataDomain.Code, String) -> ReferenceData,
): Location = Location(
  id,
  rdProvider(ReferenceDataDomain.Code.LOCATION_TYPE, typeOrDefault()) as LocationType,
  description = description,
  premise = address?.premise,
  street = address?.street,
  area = address?.area,
  city = address?.city,
  county = address?.county,
  country = address?.country,
  postcode = address?.postcode,
)

interface TemporaryAbsenceMovementRepository :
  JpaRepository<TemporaryAbsenceMovement, UUID>,
  JpaSpecificationExecutor<TemporaryAbsenceMovement> {
  fun findByLegacyId(legacyId: Long): TemporaryAbsenceMovement?
}
