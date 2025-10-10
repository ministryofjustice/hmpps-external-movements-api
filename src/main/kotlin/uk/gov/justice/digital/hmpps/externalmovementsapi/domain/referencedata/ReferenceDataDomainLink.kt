package uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.Immutable

@Immutable
@Entity
@Table(name = "reference_data_domain_link")
class ReferenceDataDomainLink(

  @Enumerated(EnumType.STRING)
  val domain: ReferenceDataDomain.Code,

  @Id
  val id: Long,
)
