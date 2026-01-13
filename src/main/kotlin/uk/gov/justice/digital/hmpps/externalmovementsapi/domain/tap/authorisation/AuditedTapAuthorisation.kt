package uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.authorisation

import jakarta.persistence.Column
import jakarta.persistence.Entity
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
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.ReasonPath
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.AccompaniedBy
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.AuthorisationStatus
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.Transport
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.absencereason.AbsenceReason
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.absencereason.AbsenceReasonCategory
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.absencereason.AbsenceSubType
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.absencereason.AbsenceType
import uk.gov.justice.digital.hmpps.externalmovementsapi.exception.NotFoundException
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Immutable
@Entity
@Table(schema = "tap", name = "audited_authorisation")
class AuditedTapAuthorisation(
  @Column(name = "person_identifier", nullable = false)
  val personIdentifier: String,
  @Column(name = "prison_code", nullable = false)
  val prisonCode: String,
  @ManyToOne(optional = false)
  @JoinColumn(name = "status_id", nullable = false)
  val status: AuthorisationStatus,
  @ManyToOne
  @JoinColumn(name = "absence_type_id")
  val absenceType: AbsenceType?,
  @ManyToOne
  @JoinColumn(name = "absence_sub_type_id")
  val absenceSubType: AbsenceSubType?,
  @ManyToOne
  @JoinColumn(name = "absence_reason_category_id")
  val absenceReasonCategory: AbsenceReasonCategory?,
  @ManyToOne
  @JoinColumn(name = "absence_reason_id")
  val absenceReason: AbsenceReason?,
  @ManyToOne(optional = false)
  @JoinColumn(name = "accompanied_by_id", nullable = false)
  val accompaniedBy: AccompaniedBy,
  @ManyToOne(optional = false)
  @JoinColumn(name = "transport_id", nullable = false)
  val transport: Transport,
  val repeat: Boolean,
  val start: LocalDate,
  val end: LocalDate,
  val comments: String?,
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "reason_path")
  val reasonPath: ReasonPath,
  @Column(name = "legacy_id")
  val legacyId: Long?,
  @Column(name = "created_at")
  val createdAt: LocalDateTime,
  @Column(name = "created_by")
  val createdBy: String,
  @Column(name = "updated_at")
  val updatedAt: LocalDateTime?,
  @Column(name = "updated_by")
  val updatedBy: String?,
  @Id
  val id: UUID = newUuid(),
)

interface AuditedTapAuthorisationRepository : JpaRepository<AuditedTapAuthorisation, UUID>

fun AuditedTapAuthorisationRepository.getAuthorisation(id: UUID) = findByIdOrNull(id) ?: throw NotFoundException("Temporary absence authorisation not found")
