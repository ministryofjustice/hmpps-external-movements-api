package uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata

import jakarta.persistence.Cacheable
import jakarta.persistence.Entity
import jakarta.persistence.QueryHint
import jakarta.persistence.Table
import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import org.hibernate.annotations.Immutable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.QueryHints
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.cache.CACHEABLE_HINT_KEY
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataBase
import uk.gov.justice.digital.hmpps.externalmovementsapi.exception.NotFoundException
import java.util.UUID

@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_ONLY)
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
  @QueryHints(QueryHint(name = CACHEABLE_HINT_KEY, value = "true"))
  fun findByCode(code: String): OccurrenceStatus?
}

fun OccurrenceStatusRepository.getByCode(code: String) = findByCode(code) ?: throw NotFoundException("Occurrence status not found")
