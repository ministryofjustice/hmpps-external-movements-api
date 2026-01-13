package uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata

import jakarta.persistence.Cacheable
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import org.hibernate.annotations.Immutable
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceData
import java.util.UUID

@Immutable
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_ONLY)
@Entity
@Table(schema = "tap", name = "accompanied_by")
class AccompaniedBy(
  @Id
  override val id: UUID,
  override val code: String,
  override val description: String,
  @Column(name = "sequence_number")
  override val sequenceNumber: Int,
  override val active: Boolean,
) : ReferenceData {
  enum class Code(val value: String) {
    NOT_PROVIDED("NOT_PROVIDED"),
    ACCOMPANIED("A"),
    UNACCOMPANIED("U"),
  }
}
