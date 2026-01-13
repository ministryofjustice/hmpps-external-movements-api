package uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata

import jakarta.persistence.Cacheable
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.QueryHint
import jakarta.persistence.Table
import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import org.hibernate.annotations.Immutable
import org.hibernate.jpa.AvailableHints
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.QueryHints
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceData
import uk.gov.justice.digital.hmpps.externalmovementsapi.exception.NotFoundException
import java.util.UUID

@Immutable
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_ONLY)
@Entity
@Table(schema = "tap", name = "authorisation_status")
class AuthorisationStatus(
  @Id
  override val id: UUID,
  override val code: String,
  override val description: String,
  @Column(name = "sequence_number")
  override val sequenceNumber: Int,
  override val active: Boolean,
) : ReferenceData {
  enum class Code {
    PENDING,
    APPROVED,
    CANCELLED,
    DENIED,
    EXPIRED,
  }
}

interface AuthorisationStatusRepository : JpaRepository<AuthorisationStatus, UUID> {
  @QueryHints(QueryHint(name = AvailableHints.HINT_CACHEABLE, value = "true"))
  fun findByCode(code: String): AuthorisationStatus?
}

fun AuthorisationStatusRepository.getByCode(code: String) = findByCode(code) ?: throw NotFoundException("Authorisation status not found")
