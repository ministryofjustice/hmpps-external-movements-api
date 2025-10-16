package uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity

@Entity
@DiscriminatorValue("ABSENCE_TYPE")
class AbsenceType(
  key: ReferenceDataKey,
  description: String,
  hintText: String?,
  sequenceNumber: Int,
  active: Boolean,
  id: Long,
) : ReferenceData(key, description, hintText, sequenceNumber, active, id)

@Entity
@DiscriminatorValue("ABSENCE_SUB_TYPE")
class AbsenceSubType(
  key: ReferenceDataKey,
  description: String,
  hintText: String?,
  sequenceNumber: Int,
  active: Boolean,
  id: Long,
) : ReferenceData(key, description, hintText, sequenceNumber, active, id)

@Entity
@DiscriminatorValue("ABSENCE_REASON_CATEGORY")
class AbsenceReasonCategory(
  key: ReferenceDataKey,
  description: String,
  hintText: String?,
  sequenceNumber: Int,
  active: Boolean,
  id: Long,
) : ReferenceData(key, description, hintText, sequenceNumber, active, id)

@Entity
@DiscriminatorValue("ABSENCE_REASON")
class AbsenceReason(
  key: ReferenceDataKey,
  description: String,
  hintText: String?,
  sequenceNumber: Int,
  active: Boolean,
  id: Long,
) : ReferenceData(key, description, hintText, sequenceNumber, active, id)

@Entity
@DiscriminatorValue("ACCOMPANIED_BY")
class AccompaniedBy(
  key: ReferenceDataKey,
  description: String,
  hintText: String?,
  sequenceNumber: Int,
  active: Boolean,
  id: Long,
) : ReferenceData(key, description, hintText, sequenceNumber, active, id) {
  enum class Code {
    NOT_PROVIDED,
  }
}

@Entity
@DiscriminatorValue("TRANSPORT")
class Transport(
  key: ReferenceDataKey,
  description: String,
  hintText: String?,
  sequenceNumber: Int,
  active: Boolean,
  id: Long,
) : ReferenceData(key, description, hintText, sequenceNumber, active, id)

@Entity
@DiscriminatorValue("TAP_AUTHORISATION_STATUS")
class TapAuthorisationStatus(
  key: ReferenceDataKey,
  description: String,
  hintText: String?,
  sequenceNumber: Int,
  active: Boolean,
  id: Long,
) : ReferenceData(key, description, hintText, sequenceNumber, active, id) {
  enum class Code {
    PENDING,
    APPROVED,
    WITHDRAWN,
    DENIED,
  }
}
