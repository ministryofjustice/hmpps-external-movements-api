package uk.gov.justice.digital.hmpps.externalmovementsapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.springframework.data.repository.findByIdOrNull
import org.springframework.transaction.support.TransactionTemplate
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.IdGenerator.newUuid
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.Location
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.TemporaryAbsenceMovement
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.TemporaryAbsenceMovementRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.TemporaryAbsenceOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.AbsenceReason
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.AccompaniedBy
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.LocationType
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceData
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_REASON
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ACCOMPANIED_BY
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.of
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.DataGenerator.personIdentifier
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.DataGenerator.prisonCode
import uk.gov.justice.digital.hmpps.externalmovementsapi.sync.TapMovementRequest
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit.SECONDS
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
      locationId: String = newUuid().toString(),
      locationType: String = LocationType.Code.OTHER.name,
      locationDescription: String? = "A location for the movement",
      locationPremise: String? = "Movement Premise",
      locationStreet: String? = "Movement Street",
      locationArea: String? = "Movement Area",
      locationCity: String? = "Movement City",
      locationCounty: String? = "Movement County",
      locationCountry: String? = "Movement Country",
      locationPostcode: String? = "Movement Postcode",
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
        location = Location(
          identifier = locationId,
          description = locationDescription,
          premise = locationPremise,
          street = locationStreet,
          area = locationArea,
          city = locationCity,
          county = locationCounty,
          country = locationCountry,
          postcode = locationPostcode,
        ),
        legacyId = legacyId,
      )
    }
  }

  fun TemporaryAbsenceMovement.verifyAgainst(personIdentifier: String, request: TapMovementRequest) {
    assertThat(this.personIdentifier).isEqualTo(personIdentifier)
    assertThat(direction.name).isEqualTo(request.direction.name)
    assertThat(occurrence?.id).isEqualTo(request.occurrenceId)
    assertThat(occurredAt).isCloseTo(request.movementDateTime, within(1, SECONDS))
    assertThat(absenceReason.code).isEqualTo(request.movementReason)
    assertThat(accompaniedBy.code).isEqualTo(request.escortOrDefault())
    assertThat(recordedByPrisonCode).isEqualTo(request.prisonCodeOrDefault())
    assertThat(location.identifier).isEqualTo(request.location.id)
    assertThat(location.description).isEqualTo(request.location.description)
    assertThat(notes).isEqualTo(request.commentText)
    assertThat(recordedBy).isEqualTo(request.audit.createUsername)
    assertThat(recordedAt).isCloseTo(request.audit.createDatetime, within(1, SECONDS))
    assertThat(legacyId).isEqualTo(request.legacyId)
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
