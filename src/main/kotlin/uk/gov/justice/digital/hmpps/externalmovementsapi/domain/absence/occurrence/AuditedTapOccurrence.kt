package uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.occurrence

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
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.authorisation.AuditedTapAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.AccompaniedBy
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.CalculatedTapOccurrenceStatus
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.Transport
import uk.gov.justice.digital.hmpps.externalmovementsapi.exception.NotFoundException
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.location.Location
import java.time.LocalDateTime
import java.util.UUID

@Immutable
@Entity
@Table(name = "audited_tap_occurrence")
class AuditedTapOccurrence(
  @ManyToOne
  @JoinColumn(name = "authorisation_id", updatable = false)
  val authorisation: AuditedTapAuthorisation,
  val personIdentifier: String,
  @ManyToOne(optional = false)
  @JoinColumn(name = "id", insertable = false, updatable = false)
  val status: CalculatedTapOccurrenceStatus,
  val releaseAt: LocalDateTime,
  val returnBy: LocalDateTime,
  @ManyToOne(optional = false)
  @JoinColumn(name = "accompanied_by_id", nullable = false)
  val accompaniedBy: AccompaniedBy,
  @ManyToOne(optional = false)
  @JoinColumn(name = "transport_id", nullable = false)
  val transport: Transport,
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "location")
  val location: Location,
  val notes: String?,
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
