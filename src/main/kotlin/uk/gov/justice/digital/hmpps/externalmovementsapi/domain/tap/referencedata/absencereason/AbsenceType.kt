package uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.absencereason

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import org.hibernate.annotations.Immutable
import org.springframework.data.jpa.repository.JpaRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.DomainLinkedReferenceData
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.FindByCode
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.ReferenceDataBase
import java.util.UUID

@Immutable
@Entity
@Table(schema = "tap", name = "absence_type")
class AbsenceType(
  code: String,
  description: String,
  sequenceNumber: Int,
  active: Boolean,
  id: UUID,
  @Enumerated(EnumType.STRING)
  override val nextDomain: ReferenceDataDomain.Code?,
) : ReferenceDataBase(code, description, sequenceNumber, active, id),
  DomainLinkedReferenceData

interface AbsenceTypeRepository :
  JpaRepository<AbsenceType, UUID>,
  FindByCode<AbsenceType> {
  override fun findByCode(code: String): AbsenceType?
}
