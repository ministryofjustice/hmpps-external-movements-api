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
import org.springframework.data.jpa.repository.Query

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
  val domain: ReferenceDataDomain.Type
  val code: String
}

@Embeddable
data class ReferenceDataKey(
  @Enumerated(EnumType.STRING)
  override val domain: ReferenceDataDomain.Type,
  override val code: String,
) : ReferenceDataLookup

infix fun ReferenceDataDomain.Type.of(code: String) = ReferenceDataKey(this, code)

interface ReferenceDataRepository : JpaRepository<ReferenceData, Long> {
  @Query(
    """
    select rd as referenceData, rddl.domain as nextDomain
    from ReferenceData rd
    left join fetch ReferenceDataDomainLink rddl on rd.id = rddl.id
    where rd.key.domain = :domain and rd.active = true
    """,
  )
  fun findWithDomainLink(domain: ReferenceDataDomain.Type): List<RdWithDomainLink>
}

interface RdWithDomainLink {
  val referenceData: ReferenceData
  val nextDomain: ReferenceDataDomain.Type?
}
