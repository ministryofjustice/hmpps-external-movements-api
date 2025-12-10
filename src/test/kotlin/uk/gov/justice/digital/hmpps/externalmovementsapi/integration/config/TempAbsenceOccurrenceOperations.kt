package uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config

import com.fasterxml.jackson.databind.JsonNode
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.springframework.data.repository.findByIdOrNull
import org.springframework.transaction.support.TransactionTemplate
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.IdGenerator.newUuid
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.ReasonPath
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.authorisation.TemporaryAbsenceAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.movement.TemporaryAbsenceMovement
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.occurrence.TemporaryAbsenceOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.occurrence.TemporaryAbsenceOccurrenceRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.AbsenceReason
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.AbsenceReasonCategory
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.AbsenceSubType
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.AbsenceType
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.AccompaniedBy
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceData
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_REASON
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_REASON_CATEGORY
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_SUB_TYPE
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_TYPE
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ACCOMPANIED_BY
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.TAP_OCCURRENCE_STATUS
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.TRANSPORT
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.TapOccurrenceStatus
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.Transport
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.of
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.DataGenerator.postcode
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.DataGenerator.word
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.CreateTapAuthorisationRequest
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.TapAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.TapOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.occurrence.CancelOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.location.Location
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit.SECONDS
import java.util.UUID

interface TempAbsenceOccurrenceOperations {
  fun givenTemporaryAbsenceOccurrence(tao: ((ReferenceDataDomain.Code, String) -> ReferenceData) -> TemporaryAbsenceOccurrence): TemporaryAbsenceOccurrence
  fun findTemporaryAbsenceOccurrence(id: UUID): TemporaryAbsenceOccurrence?
  fun findForAuthorisation(id: UUID): List<TemporaryAbsenceOccurrence>

  companion object {

    fun location(
      description: String? = word(10),
      address: String? = word(16),
      postcode: String? = postcode(),
      uprn: Long? = newUuid().mostSignificantBits,
    ) = Location(description, address, postcode, uprn)

    fun temporaryAbsenceOccurrence(
      authorisation: TemporaryAbsenceAuthorisation,
      absenceType: String? = authorisation.absenceType?.code,
      absenceSubType: String? = authorisation.absenceSubType?.code,
      absenceReasonCategory: String? = authorisation.absenceReasonCategory?.code,
      absenceReason: String? = authorisation.absenceReason?.code,
      start: LocalDateTime = LocalDateTime.now().minusDays(1),
      end: LocalDateTime = LocalDateTime.now().plusDays(1),
      location: Location = location(),
      contactInformation: String? = null,
      accompaniedBy: String = authorisation.accompaniedBy.code,
      transport: String = authorisation.transport.code,
      notes: String? = "Some notes on the occurrence",
      cancelledAt: LocalDateTime? = null,
      cancelledBy: String? = null,
      reasonPath: ReasonPath = ReasonPath(
        buildList {
          absenceType?.also { add(ABSENCE_TYPE of it) }
          absenceSubType?.also { add(ABSENCE_SUB_TYPE of it) }
          absenceReasonCategory?.also { add(ABSENCE_REASON_CATEGORY of it) }
          absenceReason?.also { add(ABSENCE_REASON of it) }
        },
      ),
      scheduleReference: JsonNode? = null,
      legacyId: Long? = null,
      movements: List<((ReferenceDataDomain.Code, String) -> ReferenceData) -> TemporaryAbsenceMovement> = listOf(),
    ): ((ReferenceDataDomain.Code, String) -> ReferenceData) -> TemporaryAbsenceOccurrence = { rdSupplier ->
      val occurrence = TemporaryAbsenceOccurrence(
        authorisation = authorisation,
        absenceType = absenceType?.let { rdSupplier(ABSENCE_TYPE, it) as AbsenceType },
        absenceSubType = absenceSubType?.let { rdSupplier(ABSENCE_SUB_TYPE, it) as AbsenceSubType },
        absenceReasonCategory = absenceReasonCategory?.let {
          rdSupplier(
            ABSENCE_REASON_CATEGORY,
            it,
          ) as AbsenceReasonCategory
        },
        absenceReason = absenceReason?.let { rdSupplier(ABSENCE_REASON, it) as AbsenceReason },
        start = start,
        end = end,
        location = location,
        contactInformation = contactInformation,
        accompaniedBy = rdSupplier(ACCOMPANIED_BY, accompaniedBy) as AccompaniedBy,
        transport = rdSupplier(TRANSPORT, transport) as Transport,
        comments = notes,
        reasonPath = reasonPath,
        scheduleReference = scheduleReference,
        legacyId = legacyId,
      )
      movements.forEach {
        occurrence.addMovement(it(rdSupplier)) { statusCode ->
          rdSupplier(TAP_OCCURRENCE_STATUS, statusCode) as TapOccurrenceStatus
        }
      }
      if (cancelledAt != null && cancelledBy != null) {
        occurrence.cancel(CancelOccurrence(), rdSupplier)
      }
      if (movements.isEmpty()) {
        occurrence.calculateStatus { rdSupplier(TAP_OCCURRENCE_STATUS, it) as TapOccurrenceStatus }
      } else {
        occurrence
      }
    }
  }

  fun TemporaryAbsenceOccurrence.verifyAgainst(
    personIdentifier: String,
    request: CreateTapAuthorisationRequest.OccurrenceRequest,
    authRequest: CreateTapAuthorisationRequest,
  ) {
    assertThat(authorisation.person.identifier).isEqualTo(personIdentifier)
    assertThat(comments).isEqualTo(authRequest.comments)
    assertThat(legacyId).isNull()
    assertThat(location).isEqualTo(request.location)
    assertThat(accompaniedBy.code).isEqualTo(authRequest.accompaniedByCode)
    assertThat(transport.code).isEqualTo(authRequest.transportCode)
    assertThat(contactInformation).isEqualTo(authRequest.contactInformation)
    assertThat(scheduleReference).isEqualTo(request.scheduleReference)
  }

  fun TemporaryAbsenceOccurrence.verifyAgainst(occurrence: TapAuthorisation.Occurrence) {
    assertThat(location).isEqualTo(occurrence.location)
    assertThat(accompaniedBy.code).isEqualTo(occurrence.accompaniedBy.code)
    assertThat(transport.code).isEqualTo(occurrence.transport.code)
    assertThat(start).isCloseTo(occurrence.start, within(2, SECONDS))
    assertThat(end).isCloseTo(occurrence.end, within(2, SECONDS))
    assertThat(absenceType?.code).isEqualTo(occurrence.absenceType?.code)
    assertThat(absenceSubType?.code).isEqualTo(occurrence.absenceSubType?.code)
    assertThat(absenceReasonCategory?.code).isEqualTo(occurrence.absenceReasonCategory?.code)
    assertThat(absenceReason?.code).isEqualTo(occurrence.absenceReason?.code)
  }

  fun TemporaryAbsenceOccurrence.verifyAgainst(occurrence: TapOccurrence) {
    assertThat(authorisation.person.identifier).isEqualTo(occurrence.authorisation.person.personIdentifier)
    assertThat(absenceType?.code).isEqualTo(occurrence.absenceType?.code)
    assertThat(absenceSubType?.code).isEqualTo(occurrence.absenceSubType?.code)
    assertThat(absenceReasonCategory?.code).isEqualTo(occurrence.absenceReasonCategory?.code)
    assertThat(absenceReason?.code).isEqualTo(occurrence.absenceReason?.code)
    assertThat(location).isEqualTo(occurrence.location)
    assertThat(accompaniedBy.code).isEqualTo(occurrence.accompaniedBy.code)
    assertThat(transport.code).isEqualTo(occurrence.transport.code)
    assertThat(start).isCloseTo(occurrence.start, within(2, SECONDS))
    assertThat(end).isCloseTo(occurrence.end, within(2, SECONDS))
    assertThat(contactInformation).isEqualTo(occurrence.contactInformation)
    assertThat(scheduleReference).isEqualTo(occurrence.scheduleReference)
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
