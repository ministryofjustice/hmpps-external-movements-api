package uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata

import jakarta.persistence.Cacheable
import jakarta.persistence.Entity
import jakarta.persistence.Table
import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import org.hibernate.annotations.Immutable
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataBase
import java.util.UUID

@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_ONLY)
@Immutable
@Entity
@Table(schema = "tap", name = "transport")
class Transport(
  code: String,
  description: String,
  sequenceNumber: Int,
  active: Boolean,
  id: UUID,
) : ReferenceDataBase(code, description, sequenceNumber, active, id)
