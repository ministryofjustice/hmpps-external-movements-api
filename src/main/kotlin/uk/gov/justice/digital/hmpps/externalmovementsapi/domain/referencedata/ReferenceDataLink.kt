package uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.annotations.Immutable
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

@Immutable
@Entity
@Table(name = "reference_data_link")
class ReferenceDataLink(
  @ManyToOne(optional = false)
  @JoinColumn(name = "reference_data_id_1")
  val rd1: ReferenceData,
  @ManyToOne(optional = false)
  @JoinColumn(name = "reference_data_id_2")
  val rd2: ReferenceData,
  val sequenceNumber: Int,
  @Id
  val id: Long,
)

interface ReferenceDataLinkRepository : JpaRepository<ReferenceDataLink, Long> {
  @Query(
    """
    select rdl.rd2 as referenceData, dl.domain as nextDomain, rdl.sequenceNumber as sequenceNumber 
    from ReferenceDataLink rdl
    left join ReferenceDataDomainLink dl on rdl.rd2.id = dl.id
    where rdl.rd1.id = :id and rdl.rd2.key.domain = :code
    """,
  )
  fun findLinksByIdAndNextDomain(id: Long, code: ReferenceDataDomain.Code): List<SequencedRdWithDomainLink>

  @EntityGraph(attributePaths = ["rd1", "rd2"])
  override fun findAll(): List<ReferenceDataLink>
}

interface SequencedRdWithDomainLink : RdWithDomainLink {
  val sequenceNumber: Int
}
