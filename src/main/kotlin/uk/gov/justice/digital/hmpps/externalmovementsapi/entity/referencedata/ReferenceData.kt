package uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata

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
import uk.gov.justice.digital.hmpps.externalmovementsapi.exception.NotFoundException
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.referencedata.CodedDescription

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
  val domain: ReferenceDataDomain.Code
  val code: String
}

@Embeddable
data class ReferenceDataKey(
  @Enumerated(EnumType.STRING)
  override val domain: ReferenceDataDomain.Code,
  override val code: String,
) : ReferenceDataLookup

infix fun ReferenceDataDomain.Code.of(code: String) = ReferenceDataKey(this, code)

interface ReferenceDataRepository : JpaRepository<ReferenceData, Long> {
  @Query(
    """
    select rd as referenceData, rddl.domain as nextDomain
    from ReferenceData rd
    left join fetch ReferenceDataDomainLink rddl on rd.id = rddl.id
    where rd.key.domain = :domain and rd.active = true
    """,
  )
  fun findWithDomainLink(domain: ReferenceDataDomain.Code): List<RdWithDomainLink>

  @Query(
    """
    select rd as referenceData, rddl.domain as nextDomain
    from ReferenceData rd
    left join fetch ReferenceDataDomainLink rddl on rd.id = rddl.id
    where rd.key.domain = :domain and rd.key.code = :code
    """,
  )
  fun findByCodeWithDomainLink(domain: ReferenceDataDomain.Code, code: String): RdWithDomainLink?

  fun findByKeyDomainAndActiveTrue(domain: ReferenceDataDomain.Code): List<ReferenceData>
}

fun ReferenceData.asCodedDescription() = CodedDescription(code, description)

interface RdWithDomainLink {
  val referenceData: ReferenceData
  val nextDomain: ReferenceDataDomain.Code?
}

fun ReferenceDataRepository.getByDomainAndCodeWithDomainLink(
  domain: ReferenceDataDomain.Code,
  code: String,
): RdWithDomainLink = findByCodeWithDomainLink(domain, code) ?: throw NotFoundException("Reference data not found")
