package uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.occurrence

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
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.authorisation.AuditedTapAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.AccompaniedBy
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.OccurrenceStatus
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.Transport
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.absencereason.AbsenceReason
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.absencereason.AbsenceReasonCategory
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.absencereason.AbsenceSubType
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.absencereason.AbsenceType
import uk.gov.justice.digital.hmpps.externalmovementsapi.exception.NotFoundException
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.location.Location
import java.time.LocalDateTime
import java.util.UUID

@Immutable
@Entity
@Table(schema = "tap", name = "audited_occurrence")
class AuditedTapOccurrence(
  @ManyToOne
  @JoinColumn(name = "authorisation_id", updatable = false, nullable = false)
  val authorisation: AuditedTapAuthorisation,
  @ManyToOne(optional = false)
  @JoinColumn(name = "status_id", nullable = false)
  val status: OccurrenceStatus,
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
  val absenceReason: AbsenceReason,
  val start: LocalDateTime,
  val end: LocalDateTime,
  @ManyToOne(optional = false)
  @JoinColumn(name = "accompanied_by_id", nullable = false)
  val accompaniedBy: AccompaniedBy,
  @ManyToOne(optional = false)
  @JoinColumn(name = "transport_id", nullable = false)
  val transport: Transport,
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "location")
  val location: Location,
  @Column(name = "contact_information")
  val contactInformation: String?,
  val comments: String?,
  val legacyId: Long?,
  val createdAt: LocalDateTime,
  val createdBy: String,
  val updatedAt: LocalDateTime?,
  val updatedBy: String?,
  @Id
  @Column(name = "id", nullable = false, updatable = false)
  val id: UUID = newUuid(),
)

interface AuditedTapOccurrenceRepository : JpaRepository<AuditedTapOccurrence, UUID> {
  fun findByAuthorisationId(authorisationId: UUID): List<AuditedTapOccurrence>
}

fun AuditedTapOccurrenceRepository.getOccurrence(id: UUID) = findByIdOrNull(id) ?: throw NotFoundException("Temporary absence occurrence not found")
