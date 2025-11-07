package uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config

import com.fasterxml.jackson.databind.JsonNode
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.springframework.data.repository.findByIdOrNull
import org.springframework.transaction.support.TransactionTemplate
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.ExternalMovementContext
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.ReasonPath
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.authorisation.TemporaryAbsenceAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.authorisation.TemporaryAbsenceAuthorisationRepository
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
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.TAP_AUTHORISATION_STATUS
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.TapAuthorisationStatus
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.of
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.DataGenerator.personIdentifier
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.DataGenerator.prisonCode
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.CreateTapAuthorisationRequest
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.TapAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.referencedata.CodedDescription
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit.SECONDS
import java.util.UUID

interface TempAbsenceAuthorisationOperations {
  fun givenTemporaryAbsenceAuthorisation(tas: ((ReferenceDataDomain.Code, String) -> ReferenceData) -> TemporaryAbsenceAuthorisation): TemporaryAbsenceAuthorisation
  fun findTemporaryAbsenceAuthorisation(id: UUID): TemporaryAbsenceAuthorisation?

  companion object {
    fun temporaryAbsenceAuthorisation(
      prisonCode: String = prisonCode(),
      personIdentifier: String = personIdentifier(),
      status: TapAuthorisationStatus.Code = TapAuthorisationStatus.Code.APPROVED,
      absenceType: String? = "SR",
      absenceSubType: String? = "RDR",
      absenceReasonCategory: String? = "PW",
      absenceReason: String? = "R15",
      accompaniedByCode: String = "L",
      repeat: Boolean = false,
      notes: String? = "Some notes on the original authorisation",
      fromDate: LocalDate = LocalDate.now().minusDays(7),
      toDate: LocalDate = LocalDate.now().minusDays(1),
      applicationDate: LocalDate = LocalDate.now().minusMonths(1),
      submittedAt: LocalDateTime = LocalDateTime.now().minusMonths(1),
      submittedBy: String = "O7h3rU53r",
      approvedAt: LocalDateTime? = null,
      approvedBy: String? = null,
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
    ): ((ReferenceDataDomain.Code, String) -> ReferenceData) -> TemporaryAbsenceAuthorisation = { rdSupplier ->
      TemporaryAbsenceAuthorisation(
        personIdentifier,
        prisonCode,
        absenceType?.let { rdSupplier(ABSENCE_TYPE, it) as AbsenceType },
        absenceSubType?.let { rdSupplier(ABSENCE_SUB_TYPE, it) as AbsenceSubType },
        absenceReasonCategory?.let { rdSupplier(ABSENCE_REASON_CATEGORY, it) as AbsenceReasonCategory },
        absenceReason?.let { rdSupplier(ABSENCE_REASON, it) as AbsenceReason },
        rdSupplier(ACCOMPANIED_BY, accompaniedByCode) as AccompaniedBy,
        repeat,
        rdSupplier(TAP_AUTHORISATION_STATUS, status.name) as TapAuthorisationStatus,
        notes,
        fromDate,
        toDate,
        submittedAt,
        submittedBy,
        approvedAt,
        approvedBy,
        reasonPath,
        schedule,
        legacyId,
      )
    }
  }

  fun TemporaryAbsenceAuthorisation.verifyAgainst(personIdentifier: String, request: CreateTapAuthorisationRequest) {
    assertThat(this.personIdentifier).isEqualTo(personIdentifier)
    assertThat(submittedAt).isCloseTo(ExternalMovementContext.get().requestAt, within(2, SECONDS))
    assertThat(status.code).isEqualTo(request.statusCode.name)
    assertThat(absenceType?.code).isEqualTo(request.absenceTypeCode)
    assertThat(absenceSubType?.code).isEqualTo(request.absenceSubTypeCode)
    assertThat(absenceReasonCategory?.code).isEqualTo(request.absenceReasonCategoryCode)
    assertThat(absenceReason?.code).isEqualTo(request.absenceReasonCode)
    assertThat(accompaniedBy.code).isEqualTo(request.accompaniedByCode)
    assertThat(notes).isEqualTo(request.notes)
    assertThat(repeat).isEqualTo(request.repeat)
    assertThat(legacyId).isNull()
    assertThat(fromDate).isEqualTo(request.fromDate)
    assertThat(toDate).isEqualTo(request.toDate)
    approvedAt?.also {
      assertThat(it).isCloseTo(request.approvedAt, within(2, SECONDS))
    }
    assertThat(schedule).isEqualTo(request.schedule)
  }

  fun TemporaryAbsenceAuthorisation.verifyAgainst(authorisation: TapAuthorisation) {
    assertThat(this.personIdentifier).isEqualTo(personIdentifier)
    assertThat(status.code).isEqualTo(authorisation.status.code)
    with(reasonPath) {
      verify(ABSENCE_TYPE, absenceType, authorisation.absenceType)
      verify(ABSENCE_SUB_TYPE, absenceSubType, authorisation.absenceSubType)
      verify(ABSENCE_REASON_CATEGORY, absenceReasonCategory, authorisation.absenceReasonCategory)
      verify(ABSENCE_REASON, absenceReason, authorisation.absenceReason)
    }
    assertThat(accompaniedBy.code).isEqualTo(authorisation.accompaniedBy.code)
    assertThat(repeat).isEqualTo(authorisation.repeat)
    assertThat(fromDate).isEqualTo(authorisation.fromDate)
    assertThat(toDate).isEqualTo(authorisation.toDate)
    assertThat(submittedAt).isCloseTo(authorisation.submitted.at, within(2, SECONDS))
    assertThat(submittedBy).isEqualTo(authorisation.submitted.by)
    approvedAt?.also {
      assertThat(it).isCloseTo(authorisation.approved!!.at, within(2, SECONDS))
    }
    assertThat(approvedBy).isEqualTo(authorisation.approved?.by)
    assertThat(schedule).isEqualTo(authorisation.schedule)
  }

  private fun ReasonPath.verify(domain: ReferenceDataDomain.Code, ef: ReferenceData?, mf: CodedDescription?) {
    if (has(domain)) {
      assertThat(ef?.code).isEqualTo(mf?.code)
    } else {
      assertThat(mf).isNull()
    }
  }
}

class TempAbsenceAuthorisationOperationsImpl(
  private val transactionTemplate: TransactionTemplate,
  private val referenceDataRepository: ReferenceDataRepository,
  private val temporaryAbsenceAuthorisationRepository: TemporaryAbsenceAuthorisationRepository,
) : TempAbsenceAuthorisationOperations {
  override fun givenTemporaryAbsenceAuthorisation(tas: ((ReferenceDataDomain.Code, String) -> ReferenceData) -> TemporaryAbsenceAuthorisation): TemporaryAbsenceAuthorisation = transactionTemplate.execute {
    val rdMap = referenceDataRepository.findAll().associateBy { it.key.domain of it.key.code }
    val authorisation: TemporaryAbsenceAuthorisation = tas { dc: ReferenceDataDomain.Code, c: String ->
      requireNotNull(rdMap[dc of c])
    }
    temporaryAbsenceAuthorisationRepository.save(authorisation)
  }!!

  override fun findTemporaryAbsenceAuthorisation(id: UUID): TemporaryAbsenceAuthorisation? = temporaryAbsenceAuthorisationRepository.findByIdOrNull(id)
}
