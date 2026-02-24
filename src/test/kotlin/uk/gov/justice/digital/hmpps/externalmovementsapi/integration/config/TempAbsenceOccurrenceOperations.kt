package uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config

import com.fasterxml.jackson.databind.JsonNode
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.springframework.data.repository.findByIdOrNull
import org.springframework.transaction.support.TransactionTemplate
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.ReasonPath
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.person.PersonSummary
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceData
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.authorisation.TemporaryAbsenceAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.authorisation.TemporaryAbsenceAuthorisationRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.authorisation.getAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.movement.TemporaryAbsenceMovement
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.occurrence.TemporaryAbsenceOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.occurrence.TemporaryAbsenceOccurrenceRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.AccompaniedBy
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.OccurrenceStatus
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.Transport
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.absencereason.AbsenceReason
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.absencereason.AbsenceReasonCategory
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.absencereason.AbsenceSubType
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.absencereason.AbsenceType
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.LocationGenerator.location
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.CreateTapAuthorisationRequest
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.TapAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.TapOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.authorisation.ChangeAuthorisationLocations
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.movement.ChangeMovementLocation
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

    fun temporaryAbsenceOccurrence(
      authorisation: TemporaryAbsenceAuthorisation,
      absenceType: String? = authorisation.absenceType?.code,
      absenceSubType: String? = authorisation.absenceSubType?.code,
      absenceReasonCategory: String? = authorisation.absenceReasonCategory?.code,
      absenceReason: String = authorisation.absenceReason.code,
      start: LocalDateTime = LocalDateTime.now().minusDays(1).truncatedTo(SECONDS),
      end: LocalDateTime = LocalDateTime.now().plusDays(1).truncatedTo(SECONDS),
      location: Location = location(),
      contactInformation: String? = null,
      accompaniedBy: String = authorisation.accompaniedBy.code,
      transport: String = authorisation.transport.code,
      comments: String? = "Some comments on the occurrence",
      cancelledAt: LocalDateTime? = null,
      cancelledBy: String? = null,
      reasonPath: ReasonPath = authorisation.reasonPath,
      scheduleReference: JsonNode? = null,
      legacyId: Long? = null,
      dpsOnly: Boolean = false,
      movements: List<((KClass<out ReferenceData>, String) -> ReferenceData, (String) -> PersonSummary) -> TemporaryAbsenceMovement> = listOf(),
    ): ((KClass<out ReferenceData>, String) -> ReferenceData) -> TemporaryAbsenceOccurrence = { rdSupplier ->
      val occurrence = TemporaryAbsenceOccurrence(
        authorisation = authorisation,
        absenceType = absenceType?.let { rdSupplier(AbsenceType::class, it) as AbsenceType },
        absenceSubType = absenceSubType?.let { rdSupplier(AbsenceSubType::class, it) as AbsenceSubType },
        absenceReasonCategory = absenceReasonCategory?.let {
          rdSupplier(AbsenceReasonCategory::class, it) as AbsenceReasonCategory
        },
        absenceReason = rdSupplier(AbsenceReason::class, absenceReason) as AbsenceReason,
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
        dpsOnly = dpsOnly,
      )
      movements.forEach {
        occurrence.addMovement(
          it(rdSupplier) { pi ->
            check(pi == authorisation.person.identifier)
            authorisation.person
          }.applyLocation(ChangeMovementLocation(location)),
        ) { statusCode -> rdSupplier(OccurrenceStatus::class, statusCode) as OccurrenceStatus }
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
    assertThat(person.identifier).isEqualTo(personIdentifier)
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
  }

  fun TemporaryAbsenceOccurrence.verifyAgainst(occurrence: TapOccurrence) {
    assertThat(person.identifier).isEqualTo(occurrence.authorisation.person.personIdentifier)
    assertThat(prisonCode).isEqualTo(occurrence.prison.code)
    assertThat(absenceType?.code).isEqualTo(occurrence.absenceType?.code)
    assertThat(absenceSubType?.code).isEqualTo(occurrence.absenceSubType?.code)
    assertThat(absenceReasonCategory?.code).isEqualTo(occurrence.absenceReasonCategory?.code)
    assertThat(absenceReason.code).isEqualTo(occurrence.absenceReason?.code)
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
  private val temporaryAbsenceAuthorisationRepository: TemporaryAbsenceAuthorisationRepository,
) : TempAbsenceOccurrenceOperations {
  override fun givenTemporaryAbsenceOccurrence(tao: ((KClass<out ReferenceData>, String) -> ReferenceData) -> TemporaryAbsenceOccurrence): TemporaryAbsenceOccurrence = transactionTemplate.execute {
    val rdMap = referenceDataRepository.findAll().associateBy { it::class to it.code }
    val occurrence: TemporaryAbsenceOccurrence = tao { dc: KClass<out ReferenceData>, c: String ->
      requireNotNull(rdMap[dc to c])
    }
    val saved = temporaryAbsenceOccurrenceRepository.saveAndFlush(occurrence)
    val locations = temporaryAbsenceOccurrenceRepository.findByAuthorisationId(saved.authorisation.id)
      .mapTo(linkedSetOf()) { it.location }
    saved.apply {
      val auth = temporaryAbsenceAuthorisationRepository.getAuthorisation(saved.authorisation.id)
      temporaryAbsenceAuthorisationRepository.save(auth.applyLocations(ChangeAuthorisationLocations(locations)))
    }
  }!!

  override fun findTemporaryAbsenceOccurrence(id: UUID): TemporaryAbsenceOccurrence? = transactionTemplate.execute {
    val occurrence = temporaryAbsenceOccurrenceRepository.findByIdOrNull(id)
    // force getting related movements inside transaction to avoid lazy loading exception in tests
    occurrence?.movements()?.forEach { it.id }
    occurrence
  }

  override fun findForAuthorisation(id: UUID): List<TemporaryAbsenceOccurrence> = temporaryAbsenceOccurrenceRepository.findByAuthorisationId(id)
}
