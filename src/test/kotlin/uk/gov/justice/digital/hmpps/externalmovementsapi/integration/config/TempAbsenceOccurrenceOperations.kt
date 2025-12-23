package uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config

import com.fasterxml.jackson.databind.JsonNode
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.springframework.data.repository.findByIdOrNull
import org.springframework.transaction.support.TransactionTemplate
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.IdGenerator.newUuid
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.ReasonPath
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_REASON
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_REASON_CATEGORY
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_SUB_TYPE
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_TYPE
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.of
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.authorisation.TemporaryAbsenceAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.movement.TemporaryAbsenceMovement
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.occurrence.TemporaryAbsenceOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.occurrence.TemporaryAbsenceOccurrenceRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.AccompaniedBy
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.OccurrenceStatus
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.ReferenceData
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.ReferenceDataRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.Transport
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.absencereason.AbsenceReason
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.absencereason.AbsenceReasonCategory
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.absencereason.AbsenceSubType
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.absencereason.AbsenceType
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
import kotlin.reflect.KClass

interface TempAbsenceOccurrenceOperations {
  fun givenTemporaryAbsenceOccurrence(tao: ((KClass<out ReferenceData>, String) -> ReferenceData) -> TemporaryAbsenceOccurrence): TemporaryAbsenceOccurrence
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
      comments: String? = "Some comments on the occurrence",
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
      movements: List<((KClass<out ReferenceData>, String) -> ReferenceData) -> TemporaryAbsenceMovement> = listOf(),
    ): ((KClass<out ReferenceData>, String) -> ReferenceData) -> TemporaryAbsenceOccurrence = { rdSupplier ->
      val occurrence = TemporaryAbsenceOccurrence(
        authorisation = authorisation,
        absenceType = absenceType?.let { rdSupplier(AbsenceType::class, it) as AbsenceType },
        absenceSubType = absenceSubType?.let { rdSupplier(AbsenceSubType::class, it) as AbsenceSubType },
        absenceReasonCategory = absenceReasonCategory?.let {
          rdSupplier(AbsenceReasonCategory::class, it) as AbsenceReasonCategory
        },
        absenceReason = absenceReason?.let { rdSupplier(AbsenceReason::class, it) as AbsenceReason },
        start = start,
        end = end,
        location = location,
        contactInformation = contactInformation,
        accompaniedBy = rdSupplier(AccompaniedBy::class, accompaniedBy) as AccompaniedBy,
        transport = rdSupplier(Transport::class, transport) as Transport,
        comments = comments,
        reasonPath = reasonPath,
        scheduleReference = scheduleReference,
        legacyId = legacyId,
      )
      movements.forEach {
        occurrence.addMovement(it(rdSupplier)) { statusCode ->
          rdSupplier(OccurrenceStatus::class, statusCode) as OccurrenceStatus
        }
      }
      if (cancelledAt != null && cancelledBy != null) {
        occurrence.cancel(CancelOccurrence(), rdSupplier)
      }
      if (movements.isEmpty()) {
        occurrence.calculateStatus { rdSupplier(OccurrenceStatus::class, it) as OccurrenceStatus }
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
  override fun givenTemporaryAbsenceOccurrence(tao: ((KClass<out ReferenceData>, String) -> ReferenceData) -> TemporaryAbsenceOccurrence): TemporaryAbsenceOccurrence = transactionTemplate.execute {
    val rdMap = referenceDataRepository.findAll().associateBy { it::class to it.code }
    val occurrence: TemporaryAbsenceOccurrence = tao { dc: KClass<out ReferenceData>, c: String ->
      requireNotNull(rdMap[dc to c])
    }
    temporaryAbsenceOccurrenceRepository.save(occurrence)
  }!!

  override fun findTemporaryAbsenceOccurrence(id: UUID): TemporaryAbsenceOccurrence? = temporaryAbsenceOccurrenceRepository.findByIdOrNull(id)

  override fun findForAuthorisation(id: UUID): List<TemporaryAbsenceOccurrence> = temporaryAbsenceOccurrenceRepository.findByAuthorisationId(id)
}
