package uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.absencereason

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.Immutable
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceData
import java.util.UUID

@Immutable
@Entity
@Table(schema = "tap", name = "absence_reason")
class AbsenceReason(
  @Id
  override val id: UUID,
  override val code: String,
  override val description: String,
  override val sequenceNumber: Int,
  override val active: Boolean,
) : ReferenceData
