package uk.gov.justice.digital.hmpps.externalmovementsapi.sync

import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.AccompaniedBy
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.LocationType
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_REASON
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ACCOMPANIED_BY
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.LOCATION_TYPE
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataRequired
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.of
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.Location
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.Location.Address
import java.time.LocalDateTime
import java.util.UUID

data class TapMovementRequest(
  val id: UUID?,
  val occurrenceId: UUID?,
  val legacyId: String,
  val movementDateTime: LocalDateTime,
  val movementReason: String,
  val direction: Direction,
  val escort: String?,
  val escortText: String?,
  val prisonCode: String?,
  val commentText: String?,
  val location: TapLocation = TapLocation(),
  val audit: NomisAudit,
) : ReferenceDataRequired {

  enum class Direction { IN, OUT }

  override fun requiredReferenceData() = setOfNotNull(
    ABSENCE_REASON of movementReason,
    ACCOMPANIED_BY of escortOrDefault(),
    LOCATION_TYPE of location.typeOrDefault(),
  )

  fun escortOrDefault(): String = escort ?: AccompaniedBy.Code.NOT_PROVIDED.name

  fun prisonCodeOrDefault(): String = prisonCode ?: DEFAULT_PRISON_CODE

  companion object {
    const val DEFAULT_PRISON_CODE = "ZZGHI"
  }
}

data class TapLocation(
  val id: String? = null,
  val typeCode: String? = null,
  val description: String? = null,
  val address: Address? = null,
) {
  fun typeOrDefault(): String = typeCode ?: LocationType.Code.OTHER.name
  fun asLocation() = Location(description, address, id)
}
