package uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.absencereason

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.Immutable
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceData
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.DomainLinkedReferenceData
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.Hintable
import java.util.UUID

@Immutable
@Entity
@Table(schema = "tap", name = "absence_sub_type")
class AbsenceSubType(
  @Id
  override val id: UUID,
  override val code: String,
  override val description: String,
  override val sequenceNumber: Int,
  override val active: Boolean,
  @Enumerated(EnumType.STRING)
  override val nextDomain: ReferenceDataDomain.Code?,
  override val hintText: String?,
) : ReferenceData,
  DomainLinkedReferenceData,
  Hintable
