package uk.gov.justice.digital.hmpps.externalmovementsapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
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
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.Transport
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.of
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.CreateTapAuthorisationRequest
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.CreateTapOccurrenceRequest
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.TapAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.TapOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.sync.ScheduledTemporaryAbsenceRequest
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit.SECONDS
import java.util.UUID

interface TempAbsenceOccurrenceOperations {
  fun givenTemporaryAbsenceOccurrence(tao: ((ReferenceDataDomain.Code, String) -> ReferenceData) -> TemporaryAbsenceOccurrence): TemporaryAbsenceOccurrence
  fun findTemporaryAbsenceOccurrence(id: UUID): TemporaryAbsenceOccurrence?
  fun findForAuthorisation(id: UUID): List<TemporaryAbsenceOccurrence>

  companion object {
    fun temporaryAbsenceOccurrence(
      authorisation: TemporaryAbsenceAuthorisation,
      releaseAt: LocalDateTime = LocalDateTime.now().minusDays(7),
      returnBy: LocalDateTime = LocalDateTime.now(),
      locationType: String = "OTHER",
      locationId: String = newUuid().toString(),
      contact: String? = null,
      accompanied: Boolean = true,
      accompaniedBy: String = "L",
      transport: String = "OD",
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
        locationType = rdSupplier(LOCATION_TYPE, locationType) as LocationType,
        locationId = locationId,
        contact = contact,
        accompaniedBy = rdSupplier(ACCOMPANIED_BY, accompaniedBy) as AccompaniedBy,
        transport = rdSupplier(TRANSPORT, transport) as Transport,
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
    request: ScheduledTemporaryAbsenceRequest,
  ) {
    assertThat(releaseAt).isCloseTo(request.startTime, within(1, SECONDS))
    assertThat(returnBy).isCloseTo(request.returnTime, within(1, SECONDS))
    assertThat(accompaniedBy.code).isEqualTo(request.escort)
    assertThat(transport.code).isEqualTo(request.transportType)
    assertThat(locationType.code).isEqualTo(request.toAddressOwnerClass ?: "OTHER")
    assertThat(locationId).isEqualTo(request.toAddressId?.toString())
    assertThat(notes).isEqualTo(request.comment)
    assertThat(legacyId).isEqualTo(request.eventId)
    assertThat(addedBy).isEqualTo(request.audit.createUsername)
    assertThat(addedAt).isCloseTo(request.audit.createDatetime, within(1, SECONDS))
  }

  fun TemporaryAbsenceOccurrence.verifyAgainst(
    personIdentifier: String,
    request: CreateTapOccurrenceRequest,
    authRequest: CreateTapAuthorisationRequest,
  ) {
    assertThat(this.personIdentifier).isEqualTo(personIdentifier)
    assertThat(notes).isEqualTo(request.notes)
    assertThat(legacyId).isNull()
    assertThat(locationType.code).isEqualTo(request.locationTypeCode)
    assertThat(accompaniedBy.code).isEqualTo(request.accompaniedByCode)
    assertThat(transport.code).isEqualTo(request.transportCode)
    assertThat(addedBy).isEqualTo(authRequest.submittedBy)
    assertThat(addedAt).isCloseTo(authRequest.submittedAt, within(1, SECONDS))
  }

  fun TemporaryAbsenceOccurrence.verifyAgainst(occurrence: TapAuthorisation.Occurrence) {
    assertThat(locationType.code).isEqualTo(occurrence.location.type.code)
    assertThat(accompaniedBy.code).isEqualTo(occurrence.accompaniedBy.code)
    assertThat(transport.code).isEqualTo(occurrence.transport.code)
    assertThat(releaseAt).isCloseTo(occurrence.releaseAt, within(1, SECONDS))
    assertThat(returnBy).isCloseTo(occurrence.returnBy, within(1, SECONDS))
  }

  fun TemporaryAbsenceOccurrence.verifyAgainst(occurrence: TapOccurrence) {
    assertThat(personIdentifier).isEqualTo(occurrence.authorisation.person.personIdentifier)
    assertThat(locationType.code).isEqualTo(occurrence.location.type.code)
    assertThat(accompaniedBy.code).isEqualTo(occurrence.accompaniedBy.code)
    assertThat(transport.code).isEqualTo(occurrence.transport.code)
    assertThat(releaseAt).isCloseTo(occurrence.releaseAt, within(1, SECONDS))
    assertThat(returnBy).isCloseTo(occurrence.returnBy, within(1, SECONDS))
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

  override fun findForAuthorisation(id: UUID): List<TemporaryAbsenceOccurrence> = temporaryAbsenceOccurrenceRepository.findByAuthorisationId(id)
}
