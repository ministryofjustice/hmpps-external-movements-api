package uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata

import jakarta.persistence.Entity
import jakarta.persistence.Table
import org.hibernate.annotations.Immutable
import org.springframework.data.jpa.repository.JpaRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.exception.NotFoundException
import java.util.UUID

@Immutable
@Entity
@Table(schema = "tap", name = "authorisation_status")
class AuthorisationStatus(
  code: String,
  description: String,
  sequenceNumber: Int,
  active: Boolean,
  id: UUID,
) : ReferenceDataBase(code, description, sequenceNumber, active, id) {
  enum class Code {
    PENDING,
    APPROVED,
    CANCELLED,
    DENIED,
    EXPIRED,
  }
}

interface AuthorisationStatusRepository : JpaRepository<AuthorisationStatus, UUID> {
  fun findByCode(code: String): AuthorisationStatus?
}

fun AuthorisationStatusRepository.getByCode(code: String) = findByCode(code) ?: throw NotFoundException("Authorisation status not found")
