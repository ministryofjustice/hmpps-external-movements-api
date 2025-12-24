package uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config

import com.fasterxml.jackson.databind.JsonNode
import org.assertj.core.api.Assertions.assertThat
import org.springframework.data.repository.findByIdOrNull
import org.springframework.transaction.support.TransactionTemplate
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.ReasonPath
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.person.PersonSummary
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceData
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_REASON
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_REASON_CATEGORY
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_SUB_TYPE
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_TYPE
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.of
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.authorisation.TemporaryAbsenceAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.authorisation.TemporaryAbsenceAuthorisationRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.AccompaniedBy
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.AuthorisationStatus
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.Transport
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.absencereason.AbsenceReason
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.absencereason.AbsenceReasonCategory
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.absencereason.AbsenceSubType
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.absencereason.AbsenceType
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.DataGenerator.personIdentifier
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.DataGenerator.prisonCode
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.CreateTapAuthorisationRequest
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.TapAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.referencedata.CodedDescription
import java.time.LocalDate
import java.util.UUID
import kotlin.reflect.KClass

interface TempAbsenceAuthorisationOperations : PersonSummaryOperations {
  fun givenTemporaryAbsenceAuthorisation(tas: ((KClass<out ReferenceData>, String) -> ReferenceData, personSupplier: (String) -> PersonSummary) -> TemporaryAbsenceAuthorisation): TemporaryAbsenceAuthorisation
  fun findTemporaryAbsenceAuthorisation(id: UUID): TemporaryAbsenceAuthorisation?

  companion object {
    fun temporaryAbsenceAuthorisation(
      prisonCode: String = prisonCode(),
      personIdentifier: String = personIdentifier(),
      status: AuthorisationStatus.Code = AuthorisationStatus.Code.APPROVED,
      absenceType: String? = "SR",
      absenceSubType: String? = "RDR",
      absenceReasonCategory: String? = "PW",
      absenceReason: String? = "R15",
      accompaniedByCode: String = "L",
      transportCode: String = "OD",
      repeat: Boolean = false,
      comments: String? = "Some comments on the original authorisation",
      start: LocalDate = LocalDate.now().minusDays(7),
      end: LocalDate = LocalDate.now().plusDays(7),
      applicationDate: LocalDate = LocalDate.now().minusWeeks(1),
      reasonPath: ReasonPath = ReasonPath(
        buildList {
          absenceType?.also { add(ABSENCE_TYPE of it) }
          absenceSubType?.also { add(ABSENCE_SUB_TYPE of it) }
          absenceReasonCategory?.also { add(ABSENCE_REASON_CATEGORY of it) }
          absenceReason?.also { add(ABSENCE_REASON of it) }
        },
      ),
      schedule: JsonNode? = null,
      legacyId: Long? = null,
    ): ((KClass<out ReferenceData>, String) -> ReferenceData, personSupplier: (String) -> PersonSummary) -> TemporaryAbsenceAuthorisation = { rdSupplier, personSupplier ->
      TemporaryAbsenceAuthorisation(
        personSupplier(personIdentifier),
        prisonCode,
        rdSupplier(AuthorisationStatus::class, status.name) as AuthorisationStatus,
        absenceType?.let { rdSupplier(AbsenceType::class, it) as AbsenceType },
        absenceSubType?.let { rdSupplier(AbsenceSubType::class, it) as AbsenceSubType },
        absenceReasonCategory?.let { rdSupplier(AbsenceReasonCategory::class, it) as AbsenceReasonCategory },
        absenceReason?.let { rdSupplier(AbsenceReason::class, it) as AbsenceReason },
        rdSupplier(AccompaniedBy::class, accompaniedByCode) as AccompaniedBy,
        rdSupplier(Transport::class, transportCode) as Transport,
        repeat,
        comments,
        start,
        end,
        reasonPath,
        schedule,
        legacyId,
      )
    }
  }

  fun TemporaryAbsenceAuthorisation.verifyAgainst(personIdentifier: String, request: CreateTapAuthorisationRequest) {
    assertThat(person.identifier).isEqualTo(personIdentifier)
    assertThat(status.code).isEqualTo(request.statusCode.name)
    assertThat(absenceType?.code).isEqualTo(request.absenceTypeCode)
    assertThat(absenceSubType?.code).isEqualTo(request.absenceSubTypeCode)
    assertThat(absenceReasonCategory?.code).isEqualTo(request.absenceReasonCategoryCode)
    assertThat(absenceReason?.code).isEqualTo(request.absenceReasonCode)
    assertThat(accompaniedBy.code).isEqualTo(request.accompaniedByCode)
    assertThat(transport.code).isEqualTo(request.transportCode)
    assertThat(comments).isEqualTo(request.comments)
    assertThat(repeat).isEqualTo(request.repeat)
    assertThat(legacyId).isNull()
    assertThat(start).isEqualTo(request.start)
    assertThat(end).isEqualTo(request.end)
    assertThat(schedule).isEqualTo(request.schedule)
  }

  fun TapAuthorisation.verifyAgainst(authorisation: TemporaryAbsenceAuthorisation) {
    assertThat(person.personIdentifier).isEqualTo(authorisation.person.identifier)
    assertThat(status.code).isEqualTo(authorisation.status.code)
    with(authorisation.reasonPath) {
      verify(ABSENCE_TYPE, absenceType, authorisation.absenceType)
      verify(ABSENCE_SUB_TYPE, absenceSubType, authorisation.absenceSubType)
      verify(ABSENCE_REASON_CATEGORY, absenceReasonCategory, authorisation.absenceReasonCategory)
      verify(ABSENCE_REASON, absenceReason, authorisation.absenceReason)
    }
    assertThat(accompaniedBy.code).isEqualTo(authorisation.accompaniedBy.code)
    assertThat(transport.code).isEqualTo(authorisation.transport.code)
    assertThat(repeat).isEqualTo(authorisation.repeat)
    assertThat(start).isEqualTo(authorisation.start)
    assertThat(end).isEqualTo(authorisation.end)
    assertThat(schedule).isEqualTo(authorisation.schedule)
    assertThat(locations).isEqualTo(occurrences.map { it.location }.distinct())
  }

  private fun ReasonPath.verify(domain: ReferenceDataDomain.Code, mf: CodedDescription?, ef: ReferenceData?) {
    if (has(domain)) {
      assertThat(mf?.code).isEqualTo(ef?.code)
    } else {
      assertThat(mf).isNull()
    }
  }
}

class TempAbsenceAuthorisationOperationsImpl(
  private val transactionTemplate: TransactionTemplate,
  private val referenceDataRepository: ReferenceDataRepository,
  private val temporaryAbsenceAuthorisationRepository: TemporaryAbsenceAuthorisationRepository,
  private val psOperations: PersonSummaryOperations,
) : TempAbsenceAuthorisationOperations,
  PersonSummaryOperations by psOperations {
  override fun givenTemporaryAbsenceAuthorisation(tas: ((KClass<out ReferenceData>, String) -> ReferenceData, personSupplier: (String) -> PersonSummary) -> TemporaryAbsenceAuthorisation): TemporaryAbsenceAuthorisation = transactionTemplate.execute {
    val rdMap = referenceDataRepository.findAll().associateBy { it::class to it.code }
    val authorisation: TemporaryAbsenceAuthorisation = tas(
      { dc: KClass<out ReferenceData>, c: String -> requireNotNull(rdMap[dc to c]) },
      { psOperations.findPersonSummary(it) ?: psOperations.givenPersonSummary(personSummary(it)) },
    )
    temporaryAbsenceAuthorisationRepository.save(authorisation)
  }!!

  override fun findTemporaryAbsenceAuthorisation(id: UUID): TemporaryAbsenceAuthorisation? = temporaryAbsenceAuthorisationRepository.findByIdOrNull(id)
}
