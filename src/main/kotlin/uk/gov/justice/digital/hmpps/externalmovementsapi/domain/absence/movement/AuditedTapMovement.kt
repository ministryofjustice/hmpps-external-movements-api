package uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.movement

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.annotations.Immutable
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.IdGenerator.newUuid
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.occurrence.TemporaryAbsenceOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.AbsenceReason
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.AccompaniedBy
import uk.gov.justice.digital.hmpps.externalmovementsapi.exception.NotFoundException
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.location.Location
import java.time.LocalDateTime
import java.util.UUID

@Immutable
@Entity
@Table(name = "audited_tap_movement")
class AuditedTapMovement(
  val personIdentifier: String,
  @ManyToOne
  @JoinColumn(name = "occurrence_id")
  val occurrence: TemporaryAbsenceOccurrence?,
  val occurredAt: LocalDateTime,
  @Enumerated(EnumType.STRING)
  val direction: TemporaryAbsenceMovement.Direction,
  @ManyToOne(optional = false)
  @JoinColumn(name = "absence_reason_id", nullable = false)
  val absenceReason: AbsenceReason,
  @ManyToOne(optional = false)
  @JoinColumn(name = "accompanied_by_id", nullable = false)
  val accompaniedBy: AccompaniedBy,
  val accompaniedByComments: String?,
  val comments: String?,
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "location")
  var location: Location,
  val recordedByPrisonCode: String,
  val legacyId: String?,
  val createdAt: LocalDateTime,
  val createdBy: String,
  val updatedAt: LocalDateTime?,
  val updatedBy: String?,
  @Id
  val id: UUID = newUuid(),
)

interface AuditedTapMovementRepository : JpaRepository<AuditedTapMovement, UUID>

fun AuditedTapMovementRepository.getMovement(id: UUID) = findByIdOrNull(id) ?: throw NotFoundException("Temporary absence movement not found")
