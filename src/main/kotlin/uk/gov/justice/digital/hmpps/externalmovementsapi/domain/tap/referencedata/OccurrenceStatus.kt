package uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata

import jakarta.persistence.Entity
import jakarta.persistence.Table
import org.hibernate.annotations.Immutable
import org.springframework.data.jpa.repository.JpaRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.exception.NotFoundException
import java.util.UUID

@Immutable
@Entity
@Table(schema = "tap", name = "occurrence_status")
class OccurrenceStatus(
  code: String,
  description: String,
  sequenceNumber: Int,
  active: Boolean,
  id: UUID,
) : ReferenceDataBase(code, description, sequenceNumber, active, id) {
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
  fun findByCode(code: String): OccurrenceStatus?
}

fun OccurrenceStatusRepository.getByCode(code: String) = findByCode(code) ?: throw NotFoundException("Occurrence status not found")
