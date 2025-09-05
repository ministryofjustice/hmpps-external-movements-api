package uk.gov.justice.digital.hmpps.externalmovementsapi.entity

import jakarta.persistence.Embeddable
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.Immutable
import org.springframework.data.jpa.repository.JpaRepository

@Immutable
@Entity
@Table(name = "reference_data")
class ReferenceData(
  @Embedded
  val key: ReferenceDataKey,
  val description: String,
  val hintText: String?,
  val sequenceNumber: Int,
  val active: Boolean,
  @Id
  val id: Long,
) : ReferenceDataLookup by key

interface ReferenceDataLookup {
  val domain: ReferenceDataDomain
  val code: String
}

@Embeddable
data class ReferenceDataKey(
  @Enumerated(EnumType.STRING)
  override val domain: ReferenceDataDomain,
  override val code: String,
) : ReferenceDataLookup

enum class ReferenceDataDomain {
  ABSENCE_TYPE,
  ABSENCE_SUB_TYPE,
  ABSENCE_REASON_CATEGORY,
  ABSENCE_REASON,
  ;

  companion object {
    fun of(domain: String): ReferenceDataDomain = entries.firstOrNull {
      it.name.lowercase().replace("_", "") == domain.lowercase().replace("[_|-]".toRegex(), "")
    } ?: throw IllegalArgumentException("Reference data domain not recognised $domain")
  }
}

infix fun ReferenceDataDomain.of(code: String) = ReferenceDataKey(this, code)

interface ReferenceDataRepository : JpaRepository<ReferenceData, Long> {
  fun findByKeyDomain(domain: ReferenceDataDomain): List<ReferenceData>
}
