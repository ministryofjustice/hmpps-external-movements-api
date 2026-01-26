package uk.gov.justice.digital.hmpps.externalmovementsapi.sync.migrate

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_REASON
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_SUB_TYPE
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_TYPE
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ACCOMPANIED_BY
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.TAP_AUTHORISATION_STATUS
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.TAP_OCCURRENCE_STATUS
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.TRANSPORT
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataKey
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataRequired
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.of
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.movement.TemporaryAbsenceMovement
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.AuthorisationStatus
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.OccurrenceStatus
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.ExternalMovementMigrated
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.TemporaryAbsenceAuthorisationMigrated
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.TemporaryAbsenceMigrated
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.location.Location
import uk.gov.justice.digital.hmpps.externalmovementsapi.sync.AtAndBy
import uk.gov.justice.digital.hmpps.externalmovementsapi.sync.AtAndByWithPrison
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class MigrateTapRequest(
  val temporaryAbsences: List<TapAuthorisation>,
  val unscheduledMovements: List<TapMovement>,
) {
  fun requiredReferenceData(): Set<ReferenceDataKey> = (
    temporaryAbsences.flatMap { auth ->
      auth.requiredReferenceData() +
        auth.occurrences.flatMap { occ ->
          occ.requiredReferenceData() +
            occ.movements.flatMap { it.requiredReferenceData() }
        }
    } + unscheduledMovements.flatMap { it.requiredReferenceData() }
    ).toSet()
}

data class MigrateTapResponse(
  val temporaryAbsences: List<MigratedAuthorisation>,
  val unscheduledMovements: List<MigratedMovement>,
)

data class MigratedAuthorisation(
  val legacyId: Long,
  val id: UUID,
  val occurrences: List<MigratedOccurrence>,
) {
  fun domainEvent(personIdentifier: String) = TemporaryAbsenceAuthorisationMigrated(personIdentifier, id)
}

data class MigratedOccurrence(
  val legacyId: Long,
  val id: UUID,
  val movements: List<MigratedMovement>,
) {
  fun domainEvent(personIdentifier: String) = TemporaryAbsenceMigrated(personIdentifier, id)
}

data class MigratedMovement(
  val legacyId: String,
  val id: UUID,
) {
  fun domainEvent(personIdentifier: String) = ExternalMovementMigrated(personIdentifier, id)
}

@Schema(name = "MigrateTapAuthorisation")
data class TapAuthorisation(
  val prisonCode: String,
  val statusCode: String,
  val absenceTypeCode: String?,
  val absenceSubTypeCode: String?,
  val absenceReasonCode: String,
  val accompaniedByCode: String,
  val transportCode: String,
  val repeat: Boolean,
  val start: LocalDate,
  val end: LocalDate,
  val comments: String?,
  val created: AtAndBy,
  val updated: AtAndBy?,
  val legacyId: Long,
  val occurrences: List<TapOccurrence>,
) : ReferenceDataRequired {
  override fun requiredReferenceData() = setOfNotNull(
    TAP_AUTHORISATION_STATUS of statusCode,
    ABSENCE_REASON of absenceReasonCode,
    absenceTypeCode?.let { ABSENCE_TYPE of it },
    absenceSubTypeCode?.let { ABSENCE_SUB_TYPE of it },
    ACCOMPANIED_BY of accompaniedByCode,
    TRANSPORT of transportCode,
    TAP_AUTHORISATION_STATUS of AuthorisationStatus.Code.EXPIRED.name,
  )
}

@Schema(name = "MigrateTapOccurrence")
data class TapOccurrence(
  val isCancelled: Boolean,
  val start: LocalDateTime,
  val end: LocalDateTime,
  val location: Location,
  val absenceTypeCode: String?,
  val absenceSubTypeCode: String?,
  val absenceReasonCode: String,
  val accompaniedByCode: String,
  val transportCode: String,
  val contactInformation: String?,
  val comments: String?,
  val created: AtAndBy,
  val updated: AtAndBy?,
  val legacyId: Long,
  val movements: List<TapMovement>,
) : ReferenceDataRequired {
  override fun requiredReferenceData() = setOfNotNull(
    ACCOMPANIED_BY of accompaniedByCode,
    TRANSPORT of transportCode,
    ABSENCE_REASON of absenceReasonCode,
    absenceTypeCode?.let { ABSENCE_TYPE of it },
    absenceSubTypeCode?.let { ABSENCE_SUB_TYPE of it },
  ) + OccurrenceStatus.Code.entries.map { TAP_OCCURRENCE_STATUS of it.name }
}

@Schema(name = "MigrateTapMovement")
data class TapMovement(
  val occurredAt: LocalDateTime,
  val direction: TemporaryAbsenceMovement.Direction,
  val prisonCode: String?,
  val absenceReasonCode: String,
  val location: Location,
  val accompaniedByCode: String,
  val accompaniedByComments: String?,
  val comments: String?,
  val created: AtAndByWithPrison,
  val updated: AtAndBy?,
  val legacyId: String,
) : ReferenceDataRequired {

  override fun requiredReferenceData() = setOfNotNull(
    ABSENCE_REASON of absenceReasonCode,
    ACCOMPANIED_BY of accompaniedByCode,
  ) + OccurrenceStatus.Code.entries.map { TAP_OCCURRENCE_STATUS of it.name }
}
