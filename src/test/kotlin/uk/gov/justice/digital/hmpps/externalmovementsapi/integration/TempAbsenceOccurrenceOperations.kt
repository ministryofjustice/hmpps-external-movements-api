package uk.gov.justice.digital.hmpps.externalmovementsapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.springframework.data.repository.findByIdOrNull
import org.springframework.transaction.support.TransactionTemplate
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.IdGenerator.newUuid
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.TemporaryAbsenceAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.TemporaryAbsenceOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.TemporaryAbsenceOccurrenceRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.AccompaniedBy
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.LocationType
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.ReferenceData
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.ReferenceDataDomain
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.ReferenceDataDomain.Code.ACCOMPANIED_BY
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.ReferenceDataDomain.Code.LOCATION_TYPE
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.ReferenceDataDomain.Code.TRANSPORT
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.ReferenceDataRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.TapAuthorisationStatus
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.TapOccurrenceStatus
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.Transport
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.of
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.CreateTapAuthorisationRequest
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.CreateTapOccurrenceRequest
import java.time.LocalDateTime
import java.util.UUID

interface TempAbsenceOccurrenceOperations {
  fun givenTemporaryAbsenceOccurrence(tao: ((ReferenceDataDomain.Code, String) -> ReferenceData) -> TemporaryAbsenceOccurrence): TemporaryAbsenceOccurrence
  fun findTemporaryAbsenceOccurrence(id: UUID): TemporaryAbsenceOccurrence?

  companion object {
    fun temporaryAbsenceOccurrence(
      authorisation: TemporaryAbsenceAuthorisation,
      status: TapOccurrenceStatus.Code = TapOccurrenceStatus.Code.SCHEDULED,
      releaseAt: LocalDateTime = LocalDateTime.now().minusDays(7),
      returnBy: LocalDateTime = LocalDateTime.now(),
      locationType: String = "OTHER",
      locationId: String = newUuid().toString(),
      contact: String? = null,
      accompanied: Boolean = true,
      accompaniedBy: String? = "L",
      transport: String? = "OD",
      notes: String? = "Some notes on the occurrence",
      addedAt: LocalDateTime = LocalDateTime.now().minusMonths(1),
      addedBy: String = "O7h3rU53r",
      cancelledAt: LocalDateTime? = null,
      cancelledBy: String? = null,
      legacyId: Long? = null,
    ): ((ReferenceDataDomain.Code, String) -> ReferenceData) -> TemporaryAbsenceOccurrence = { rdSupplier ->
      TemporaryAbsenceOccurrence(
        authorisation = authorisation,
        releaseAt = releaseAt,
        returnBy = returnBy,
        status = rdSupplier(ReferenceDataDomain.Code.TAP_OCCURRENCE_STATUS, status.name) as TapOccurrenceStatus,
        locationType = rdSupplier(LOCATION_TYPE, locationType) as LocationType,
        locationId = locationId,
        contact = contact,
        accompaniedBy = accompaniedBy?.let { rdSupplier(ACCOMPANIED_BY, it) as AccompaniedBy },
        transport = transport?.let { rdSupplier(TRANSPORT, it) as Transport },
        notes = notes,
        addedAt = addedAt,
        addedBy = addedBy,
        cancelledAt = cancelledAt,
        cancelledBy = cancelledBy,
        legacyId = legacyId,
      )
    }
  }

  fun TemporaryAbsenceOccurrence.verifyAgainst(
    personIdentifier: String,
    request: CreateTapOccurrenceRequest,
    authRequest: CreateTapAuthorisationRequest,
  ) {
    assertThat(this.personIdentifier).isEqualTo(personIdentifier)
    val expectedStatus = when (authRequest.statusCode) {
      TapAuthorisationStatus.Code.PENDING -> TapOccurrenceStatus.Code.PENDING
      TapAuthorisationStatus.Code.APPROVED -> TapOccurrenceStatus.Code.SCHEDULED
    }
    assertThat(status.code).isEqualTo(expectedStatus.name)
    assertThat(notes).isEqualTo(request.notes)
    assertThat(legacyId).isNull()
    assertThat(addedBy).isEqualTo(authRequest.submittedBy)
    assertThat(addedAt).isEqualTo(authRequest.submittedAt)
  }
}

class TempAbsenceOccurrenceOperationsImpl(
  private val transactionTemplate: TransactionTemplate,
  private val referenceDataRepository: ReferenceDataRepository,
  private val temporaryAbsenceOccurrenceRepository: TemporaryAbsenceOccurrenceRepository,
) : TempAbsenceOccurrenceOperations {
  override fun givenTemporaryAbsenceOccurrence(tao: ((ReferenceDataDomain.Code, String) -> ReferenceData) -> TemporaryAbsenceOccurrence): TemporaryAbsenceOccurrence = transactionTemplate.execute {
    val rdMap = referenceDataRepository.findAll().associateBy { it.key.domain of it.key.code }
    val occurrence: TemporaryAbsenceOccurrence = tao { dc: ReferenceDataDomain.Code, c: String ->
      requireNotNull(rdMap[dc of c])
    }
    temporaryAbsenceOccurrenceRepository.save(occurrence)
  }!!

  override fun findTemporaryAbsenceOccurrence(id: UUID): TemporaryAbsenceOccurrence? = temporaryAbsenceOccurrenceRepository.findByIdOrNull(id)
}
