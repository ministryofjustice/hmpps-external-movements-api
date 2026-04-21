package uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.absencereason

import jakarta.persistence.Cacheable
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import org.hibernate.annotations.Immutable
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.DomainLinkedReferenceData
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.Hintable
import java.util.UUID

@Immutable
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_ONLY)
@Entity
@Table(schema = "tap", name = "absence_sub_type")
class AbsenceSubType(
  code: String,
  description: String,
  sequenceNumber: Int,
  active: Boolean,
  id: UUID,
  nextDomain: ReferenceDataDomain.Code?,
  @Column(name = "hint_text")
  override val hintText: String?,
  @JdbcTypeCode(SqlTypes.ARRAY)
  @Column(name = "groups", nullable = false, columnDefinition = "varchar[]")
  val groups: Array<String> = arrayOf(),
) : DomainLinkedReferenceData(code, description, sequenceNumber, active, nextDomain, id),
  Hintable {
  enum class Code(val value: String) {
    SECURITY_ESCORT("SE"),
  }
}
