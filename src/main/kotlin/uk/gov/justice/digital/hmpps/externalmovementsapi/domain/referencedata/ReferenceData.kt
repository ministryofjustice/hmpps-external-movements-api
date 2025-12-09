package uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata

import jakarta.persistence.Column
import jakarta.persistence.DiscriminatorColumn
import jakarta.persistence.DiscriminatorType
import jakarta.persistence.Embeddable
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Inheritance
import jakarta.persistence.InheritanceType
import jakarta.persistence.Table
import org.hibernate.annotations.Immutable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import uk.gov.justice.digital.hmpps.externalmovementsapi.exception.NotFoundException
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.referencedata.CodedDescription

@Immutable
@Entity
@Table(name = "reference_data")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "domain", discriminatorType = DiscriminatorType.STRING)
open class ReferenceData(
  @Embedded
  val key: ReferenceDataKey,
  val description: String,
  val hintText: String?,
  val sequenceNumber: Int,
  val active: Boolean,
  @Id
  val id: Long,
) : ReferenceDataLookup by key {
  companion object {
    val KEY = ReferenceData::key.name
    val SEQUENCE_NUMBER = ReferenceData::sequenceNumber.name
  }
}

interface ReferenceDataLookup {
  val domain: ReferenceDataDomain.Code
  val code: String
}

@Embeddable
data class ReferenceDataKey(
  @Enumerated(EnumType.STRING)
  @Column(insertable = false, updatable = false)
  override val domain: ReferenceDataDomain.Code,
  @Column(insertable = false, updatable = false)
  override val code: String,
) : ReferenceDataLookup {
  companion object {
    val CODE = ReferenceDataKey::code.name
  }
}

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
    where rd.key in :keys
    """,
  )
  fun findMatchingWithDomainLink(keys: Set<ReferenceDataKey>): List<RdWithDomainLink>

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

  fun findByKeyIn(keys: Set<ReferenceDataKey>): List<ReferenceData>

  fun findByKey(key: ReferenceDataKey): ReferenceData?

  @Query(
    """
    select rdl.rd2 as referenceData from ReferenceDataLink rdl
    left join ReferenceDataDomainLink dl on rdl.rd2.id = dl.id
    where rdl.rd1.id = :id and rdl.rd2.key.domain = :nextDomain
    """,
  )
  fun findLinkedItems(nextDomain: ReferenceDataDomain.Code, id: Long): List<ReferenceData>

  @Query(
    """
    select rdl.rd1 as referenceData from ReferenceDataLink rdl
    left join ReferenceDataDomainLink dl on rdl.rd1.id = dl.id
    where rdl.rd2.id = :id
    """,
  )
  fun findLinkedFrom(id: Long): List<ReferenceData>
}

fun ReferenceDataRepository.getByKey(key: ReferenceDataKey): ReferenceData = findByKey(key) ?: throw NotFoundException("${key.domain} not found")

fun ReferenceDataRepository.findRdWithPaths(rdr: ReferenceDataRequired): ReferenceDataPaths = ReferenceDataPaths(findMatchingWithDomainLink(rdr.requiredReferenceData())) { id: Long -> findLinkedFrom(id) }

fun ReferenceDataRepository.rdProvider(rdr: ReferenceDataRequired): (ReferenceDataDomain.Code, String) -> ReferenceData {
  val rdMap = findByKeyIn(rdr.requiredReferenceData()).associateBy { it.key }
  return { dc: ReferenceDataDomain.Code, c: String -> requireNotNull(rdMap[dc of c]) }
}

fun ReferenceData.asCodedDescription() = CodedDescription(code, description, hintText)

interface RdWithDomainLink {
  val referenceData: ReferenceData
  val nextDomain: ReferenceDataDomain.Code?
  val domain: ReferenceDataDomain.Code get() = referenceData.domain
}

fun ReferenceDataRepository.getByDomainAndCodeWithDomainLink(
  domain: ReferenceDataDomain.Code,
  code: String,
): RdWithDomainLink = findByCodeWithDomainLink(domain, code) ?: throw NotFoundException("Reference data not found")
