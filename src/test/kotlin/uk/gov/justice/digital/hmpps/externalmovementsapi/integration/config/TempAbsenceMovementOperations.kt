package uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config

import org.springframework.data.repository.findByIdOrNull
import org.springframework.transaction.support.TransactionTemplate
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceData
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.movement.TemporaryAbsenceMovement
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.movement.TemporaryAbsenceMovementRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.occurrence.TemporaryAbsenceOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.AccompaniedBy
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.absencereason.AbsenceReason
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.DataGenerator.personIdentifier
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.DataGenerator.prisonCode
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.location.Location
import java.time.LocalDateTime
import java.util.UUID
import kotlin.reflect.KClass

interface TempAbsenceMovementOperations {
  fun givenTemporaryAbsenceMovement(tam: ((KClass<out ReferenceData>, String) -> ReferenceData) -> TemporaryAbsenceMovement): TemporaryAbsenceMovement
  fun findTemporaryAbsenceMovement(id: UUID): TemporaryAbsenceMovement?

  companion object {
    fun temporaryAbsenceMovement(
      direction: TemporaryAbsenceMovement.Direction,
      personIdentifier: String = personIdentifier(),
      occurrence: TemporaryAbsenceOccurrence? = null,
      occurredAt: LocalDateTime = LocalDateTime.now().minusDays(7),
      absenceReason: String = "R15",
      accompaniedBy: String = AccompaniedBy.Code.NOT_PROVIDED.name,
      accompaniedByComments: String? = "Some comments about the accompanied by",
      comments: String? = "Some comments on the movement",
      recordedByPrison: String = prisonCode(),
      location: Location = TempAbsenceOccurrenceOperations.location(),
      legacyId: String? = null,
    ): ((KClass<out ReferenceData>, String) -> ReferenceData) -> TemporaryAbsenceMovement = { rdSupplier ->
      TemporaryAbsenceMovement(
        personIdentifier = personIdentifier,
        occurrence = occurrence,
        direction = direction,
        occurredAt = occurredAt,
        absenceReason = rdSupplier(AbsenceReason::class, absenceReason) as AbsenceReason,
        accompaniedBy = rdSupplier(AccompaniedBy::class, accompaniedBy) as AccompaniedBy,
        accompaniedByComments = accompaniedByComments,
        comments = comments,
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
  override fun givenTemporaryAbsenceMovement(tam: ((KClass<out ReferenceData>, String) -> ReferenceData) -> TemporaryAbsenceMovement): TemporaryAbsenceMovement = transactionTemplate.execute {
    val rdMap = referenceDataRepository.findAll().associateBy { it::class to it.code }
    val movement: TemporaryAbsenceMovement = tam { dc: KClass<out ReferenceData>, c: String ->
      requireNotNull(rdMap[dc to c])
    }
    temporaryAbsenceMovementRepository.save(movement)
  }!!

  override fun findTemporaryAbsenceMovement(id: UUID): TemporaryAbsenceMovement? = temporaryAbsenceMovementRepository.findByIdOrNull(id)
}
