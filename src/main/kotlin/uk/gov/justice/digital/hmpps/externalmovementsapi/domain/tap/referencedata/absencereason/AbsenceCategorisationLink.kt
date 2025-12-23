package uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.absencereason

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.Immutable
import org.springframework.data.jpa.repository.JpaRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain
import java.util.UUID

@Immutable
@Entity
@Table(schema = "tap", name = "absence_categorisation_link")
class AbsenceCategorisationLink(
  @Enumerated(EnumType.STRING)
  @Column(name = "domain_1")
  val domain1: ReferenceDataDomain.Code,
  @Column(name = "id_1")
  val id1: UUID,
  @Enumerated(EnumType.STRING)
  @Column(name = "domain_2")
  val domain2: ReferenceDataDomain.Code,
  @Column(name = "id_2")
  val id2: UUID,
  @Column(name = "sequence_number")
  val sequenceNumber: Int,
  @Id
  val id: UUID,
)

interface AbsenceCategorisationLinkRepository : JpaRepository<AbsenceCategorisationLink, UUID> {
  fun findById1AndDomain2(id: UUID, domainCode: ReferenceDataDomain.Code): List<AbsenceCategorisationLink>
}
