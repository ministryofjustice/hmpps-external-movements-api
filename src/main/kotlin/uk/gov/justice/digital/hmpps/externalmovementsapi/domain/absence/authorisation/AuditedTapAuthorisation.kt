package uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.authorisation

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
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.AbsenceReason
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.AbsenceReasonCategory
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.AbsenceSubType
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.AbsenceType
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.AccompaniedBy
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.TapAuthorisationStatus
import uk.gov.justice.digital.hmpps.externalmovementsapi.exception.NotFoundException
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Immutable
@Entity
@Table(name = "audited_tap_authorisation")
class AuditedTapAuthorisation(
  val personIdentifier: String,
  val prisonCode: String,
  @ManyToOne(optional = false)
  @JoinColumn(name = "status_id", nullable = false)
  val status: TapAuthorisationStatus,
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
  val repeat: Boolean,
  val fromDate: LocalDate,
  val toDate: LocalDate,
  val notes: String?,
  @JdbcTypeCode(SqlTypes.JSON)
  val reasonPath: ReasonPath,
  val legacyId: Long?,
  val createdAt: LocalDateTime,
  val createdBy: String,
  val updatedAt: LocalDateTime?,
  val updatedBy: String?,
  @Id
  val id: UUID = newUuid(),
)

interface AuditedTapAuthorisationRepository : JpaRepository<AuditedTapAuthorisation, UUID>

fun AuditedTapAuthorisationRepository.getAuthorisation(id: UUID) = findByIdOrNull(id) ?: throw NotFoundException("Temporary absence authorisation not found")
