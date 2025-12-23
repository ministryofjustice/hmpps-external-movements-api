package uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.absencereason

import jakarta.persistence.Entity
import jakarta.persistence.Table
import org.hibernate.annotations.Immutable
import org.springframework.data.jpa.repository.JpaRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.ReferenceDataBase
import java.util.UUID

@Immutable
@Entity
@Table(schema = "tap", name = "absence_reason")
class AbsenceReason(
  code: String,
  description: String,
  sequenceNumber: Int,
  active: Boolean,
  id: UUID,
) : ReferenceDataBase(code, description, sequenceNumber, active, id)

interface AbsenceReasonRepository : JpaRepository<AbsenceReason, UUID>
