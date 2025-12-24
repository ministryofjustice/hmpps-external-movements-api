package uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.QueryHint
import jakarta.persistence.Table
import org.hibernate.annotations.Immutable
import org.hibernate.jpa.AvailableHints
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.QueryHints
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceData
import uk.gov.justice.digital.hmpps.externalmovementsapi.exception.NotFoundException
import java.util.UUID

@Immutable
@Entity
@Table(schema = "tap", name = "occurrence_status")
class OccurrenceStatus(
  @Id
  override val id: UUID,
  override val code: String,
  override val description: String,
  override val sequenceNumber: Int,
  override val active: Boolean,
) : ReferenceData {
  enum class Code {
    PENDING,
    SCHEDULED,
    IN_PROGRESS,
    COMPLETED,
    OVERDUE,
    EXPIRED,
    CANCELLED,
    DENIED,
  }
}

interface OccurrenceStatusRepository : JpaRepository<OccurrenceStatus, UUID> {
  @QueryHints(QueryHint(name = AvailableHints.HINT_CACHEABLE, value = "true"))
  fun findByCode(code: String): OccurrenceStatus?
}

fun OccurrenceStatusRepository.getByCode(code: String) = findByCode(code) ?: throw NotFoundException("Occurrence status not found")
