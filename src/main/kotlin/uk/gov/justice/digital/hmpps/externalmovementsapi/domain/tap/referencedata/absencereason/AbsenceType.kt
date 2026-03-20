package uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.absencereason

import jakarta.persistence.Cacheable
import jakarta.persistence.Entity
import jakarta.persistence.Table
import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import org.hibernate.annotations.Immutable
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.DomainLinkedReferenceData
import java.util.UUID

@Immutable
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_ONLY)
@Entity
@Table(schema = "tap", name = "absence_type")
class AbsenceType(
  code: String,
  description: String,
  sequenceNumber: Int,
  active: Boolean,
  nextDomain: ReferenceDataDomain.Code?,
  id: UUID,
) : DomainLinkedReferenceData(code, description, sequenceNumber, active, nextDomain, id) {
  enum class Code(val value: String) {
    SECURITY_ESCORT("SE"),
  }
}
