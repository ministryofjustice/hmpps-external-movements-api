package uk.gov.justice.digital.hmpps.externalmovementsapi.entity

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.Immutable
import org.springframework.data.jpa.repository.JpaRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.exception.NotFoundException

@Immutable
@Entity
@Table(name = "reference_data_domain")
class ReferenceDataDomain(
  @Id
  @Enumerated(EnumType.STRING)
  val code: Type,
  val description: String,
) {
  enum class Type {
    ABSENCE_TYPE,
    ABSENCE_SUB_TYPE,
    ABSENCE_REASON_CATEGORY,
    ABSENCE_REASON,
    ;

    companion object {
      fun of(domain: String): Type = entries.firstOrNull {
        it.name.lowercase().replace("_", "") == domain.lowercase().replace("[_|-]".toRegex(), "")
      } ?: throw NotFoundException("Reference data domain not found")
    }
  }
}

interface ReferenceDataDomainRepository : JpaRepository<ReferenceDataDomain, ReferenceDataDomain.Type>
