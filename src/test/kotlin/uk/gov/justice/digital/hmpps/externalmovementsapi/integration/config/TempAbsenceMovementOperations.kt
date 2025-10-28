package uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config

import org.springframework.data.repository.findByIdOrNull
import org.springframework.transaction.support.TransactionTemplate
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.TemporaryAbsenceMovement
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.TemporaryAbsenceMovementRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.TemporaryAbsenceOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.AbsenceReason
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.AccompaniedBy
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceData
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_REASON
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ACCOMPANIED_BY
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.of
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.DataGenerator.personIdentifier
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.DataGenerator.prisonCode
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.location.Location
import java.time.LocalDateTime
import java.util.UUID

interface TempAbsenceMovementOperations {
  fun givenTemporaryAbsenceMovement(tam: ((ReferenceDataDomain.Code, String) -> ReferenceData) -> TemporaryAbsenceMovement): TemporaryAbsenceMovement
  fun findTemporaryAbsenceMovement(id: UUID): TemporaryAbsenceMovement?

  companion object {
    fun temporaryAbsenceMovement(
      direction: TemporaryAbsenceMovement.Direction,
      personIdentifier: String = personIdentifier(),
      occurrence: TemporaryAbsenceOccurrence? = null,
      occurredAt: LocalDateTime = LocalDateTime.now().minusDays(7),
      absenceReason: String = "R15",
      accompaniedBy: String = AccompaniedBy.Code.NOT_PROVIDED.name,
      accompaniedByNotes: String? = "Some notes about the accompanied by",
      notes: String? = "Some notes on the movement",
      recordedAt: LocalDateTime = LocalDateTime.now().minusMonths(1),
      recordedBy: String = "O7h3rU53r",
      recordedByPrison: String = prisonCode(),
      location: Location = TempAbsenceOccurrenceOperations.location(),
      legacyId: String? = null,
    ): ((ReferenceDataDomain.Code, String) -> ReferenceData) -> TemporaryAbsenceMovement = { rdSupplier ->
      TemporaryAbsenceMovement(
        personIdentifier = personIdentifier,
        occurrence = occurrence,
        direction = direction,
        occurredAt = occurredAt,
        absenceReason = rdSupplier(ABSENCE_REASON, absenceReason) as AbsenceReason,
        accompaniedBy = rdSupplier(ACCOMPANIED_BY, accompaniedBy) as AccompaniedBy,
        accompaniedByNotes = accompaniedByNotes,
        notes = notes,
        recordedAt = recordedAt,
        recordedBy = recordedBy,
        recordedByPrisonCode = recordedByPrison,
        location = location,
        legacyId = legacyId,
      )
    }
  }
}

class TempAbsenceMovementOperationsImpl(
  private val transactionTemplate: TransactionTemplate,
  private val referenceDataRepository: ReferenceDataRepository,
  private val temporaryAbsenceMovementRepository: TemporaryAbsenceMovementRepository,
) : TempAbsenceMovementOperations {
  override fun givenTemporaryAbsenceMovement(tam: ((ReferenceDataDomain.Code, String) -> ReferenceData) -> TemporaryAbsenceMovement): TemporaryAbsenceMovement = transactionTemplate.execute {
    val rdMap = referenceDataRepository.findAll().associateBy { it.key.domain of it.key.code }
    val movement: TemporaryAbsenceMovement = tam { dc: ReferenceDataDomain.Code, c: String ->
      requireNotNull(rdMap[dc of c])
    }
    temporaryAbsenceMovementRepository.save(movement)
  }!!

  override fun findTemporaryAbsenceMovement(id: UUID): TemporaryAbsenceMovement? = temporaryAbsenceMovementRepository.findByIdOrNull(id)
}
