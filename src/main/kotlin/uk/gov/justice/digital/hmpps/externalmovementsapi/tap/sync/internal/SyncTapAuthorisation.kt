package uk.gov.justice.digital.hmpps.externalmovementsapi.tap.sync.internal

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.ExternalMovementContext
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.ExternalMovementContext.Companion.SYSTEM_USERNAME
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.set
import uk.gov.justice.digital.hmpps.externalmovementsapi.exception.ConflictException
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.domain.authorisation.TemporaryAbsenceAuthorisationRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.domain.occurrence.TemporaryAbsenceOccurrenceRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.sync.write.SyncResponse
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.sync.write.TapAuthorisation
import java.util.UUID

@Transactional
@Service
class SyncTapAuthorisation(
  private val authorisationRepository: TemporaryAbsenceAuthorisationRepository,
  private val occurrenceRepository: TemporaryAbsenceOccurrenceRepository,
) {
  fun sync(request: TapAuthorisation): Pair<SyncResponse, SyncRequestStatus> {
    val auth = (
      request.id?.let { authorisationRepository.findByIdOrNull(it) }
        ?: authorisationRepository.findByLegacyId(request.legacyId)
      )
    val status: SyncRequestStatus =
      auth?.let { SyncRequestStatus.Actionable(ActionType.UPDATE, EntityType.PLAN) }
        ?: let { SyncRequestStatus.Actionable(ActionType.CREATE, EntityType.PLAN) }

    return auth?.id?.let {
      SyncResponse(it) to status
    } ?: throw ConflictException("Create authorisation request not expected")
  }

  fun deleteById(id: UUID): SyncRequestStatus {
    ExternalMovementContext.get().copy(username = SYSTEM_USERNAME).set()
    return authorisationRepository.findByIdOrNull(id)?.let { authorisation ->
      val occurrenceCount = if (authorisation.repeat) {
        occurrenceRepository.countByAuthorisationId(authorisation.id)
      } else {
        val occ = occurrenceRepository.findByAuthorisationId(authorisation.id).singleOrNull()
        when {
          occ == null -> 0
          occ.dpsOnly -> {
            occurrenceRepository.delete(occ)
            0
          }

          else -> 1
        }
      }
      if (occurrenceCount > 0) {
        SyncRequestStatus.NotActionable(
          ActionType.DELETE,
          EntityType.PLAN,
          "Authorisation has $occurrenceCount occurrences",
        )
      } else {
        SyncRequestStatus.Actionable(ActionType.DELETE, EntityType.PLAN)
      }
    } ?: SyncRequestStatus.NotFound(ActionType.DELETE, EntityType.PLAN)
  }
}
