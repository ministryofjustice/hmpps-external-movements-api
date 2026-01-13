package uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.absencereason

import jakarta.persistence.Cacheable
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import org.hibernate.annotations.Immutable
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceData
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.DomainLinkedReferenceData
import java.util.UUID

@Immutable
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_ONLY)
@Entity
@Table(schema = "tap", name = "absence_reason_category")
class AbsenceReasonCategory(
  @Id
  override val id: UUID,
  override val code: String,
  override val description: String,
  @Column(name = "sequence_number")
  override val sequenceNumber: Int,
  override val active: Boolean,
  @Enumerated(EnumType.STRING)
  @Column(name = "next_domain")
  override val nextDomain: ReferenceDataDomain.Code?,
) : ReferenceData,
  DomainLinkedReferenceData
