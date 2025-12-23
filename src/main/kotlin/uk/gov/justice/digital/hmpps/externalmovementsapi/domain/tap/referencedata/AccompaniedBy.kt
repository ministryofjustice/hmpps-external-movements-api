package uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata

import jakarta.persistence.Entity
import jakarta.persistence.Table
import org.hibernate.annotations.Immutable
import java.util.UUID

@Immutable
@Entity
@Table(schema = "tap", name = "accompanied_by")
class AccompaniedBy(
  code: String,
  description: String,
  sequenceNumber: Int,
  active: Boolean,
  id: UUID,
) : ReferenceDataBase(code, description, sequenceNumber, active, id) {
  enum class Code(val value: String) {
    NOT_PROVIDED("NOT_PROVIDED"),
    ACCOMPANIED("A"),
    UNACCOMPANIED("U"),
  }
}
