package uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata

import jakarta.persistence.Column
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.MappedSuperclass
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceData
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain
import java.util.UUID

@MappedSuperclass
abstract class DomainLinkedReferenceData(
  code: String,
  description: String,
  sequenceNumber: Int,
  active: Boolean,
  @Column(name = "next_domain")
  @Enumerated(EnumType.STRING)
  val nextDomain: ReferenceDataDomain.Code?,
  id: UUID,
) : ReferenceData(code, description, sequenceNumber, active, id)
