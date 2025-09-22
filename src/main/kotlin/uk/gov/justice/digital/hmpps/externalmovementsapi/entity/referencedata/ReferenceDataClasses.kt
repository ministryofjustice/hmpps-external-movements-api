package uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata

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
) : ReferenceData(key, description, hintText, sequenceNumber, active, id)

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
@DiscriminatorValue("TAP_STATUS")
class TapStatus(
  key: ReferenceDataKey,
  description: String,
  hintText: String?,
  sequenceNumber: Int,
  active: Boolean,
  id: Long,
) : ReferenceData(key, description, hintText, sequenceNumber, active, id) {
  enum class Code(val value: String) {
    PENDING("PEN"),
    APPROVED_SCHEDULED("APP-SCH"),
    APPROVED_UNSCHEDULED("APP-UNSCH"),
  }
}

@Entity
@DiscriminatorValue("LOCATION_TYPE")
class LocationType(
  key: ReferenceDataKey,
  description: String,
  hintText: String?,
  sequenceNumber: Int,
  active: Boolean,
  id: Long,
) : ReferenceData(key, description, hintText, sequenceNumber, active, id)
