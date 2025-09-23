package uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.Immutable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.externalmovementsapi.exception.NotFoundException

@Immutable
@Entity
@Table(name = "reference_data_domain")
class ReferenceDataDomain(
  @Id
  @Enumerated(EnumType.STRING)
  val code: Code,
  val description: String,
) {
  enum class Code {
    ABSENCE_TYPE,
    ABSENCE_SUB_TYPE,
    ABSENCE_REASON_CATEGORY,
    ABSENCE_REASON,

    ACCOMPANIED_BY,
    TRANSPORT,
    TAP_AUTHORISATION_STATUS,
    LOCATION_TYPE,
    ;

    companion object {
      fun of(domain: String): Code = entries.firstOrNull {
        it.name.lowercase().replace("_", "") == domain.lowercase().replace("[_|-]".toRegex(), "")
      } ?: throw NotFoundException("Reference data domain not found")
    }
  }
}

interface ReferenceDataDomainRepository : JpaRepository<ReferenceDataDomain, ReferenceDataDomain.Code>

fun ReferenceDataDomainRepository.getDomain(code: ReferenceDataDomain.Code): ReferenceDataDomain = findByIdOrNull(code) ?: throw NotFoundException("Reference data domain not found")
