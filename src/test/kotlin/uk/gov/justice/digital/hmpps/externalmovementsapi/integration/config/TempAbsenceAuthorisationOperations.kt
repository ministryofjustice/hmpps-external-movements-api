package uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config

import com.fasterxml.jackson.databind.JsonNode
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.springframework.data.repository.findByIdOrNull
import org.springframework.transaction.support.TransactionTemplate
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.TemporaryAbsenceAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.TemporaryAbsenceAuthorisationRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.AbsenceReason
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.AbsenceSubType
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.AbsenceType
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceData
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_REASON
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_SUB_TYPE
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_TYPE
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.TAP_AUTHORISATION_STATUS
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.TapAuthorisationStatus
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.of
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.DataGenerator.personIdentifier
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.DataGenerator.prisonCode
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.CreateTapAuthorisationRequest
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.TapAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.sync.TapApplicationRequest
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
      absenceReason: String = "R15",
      repeat: Boolean = false,
      notes: String? = "Some notes on the original authorisation",
      fromDate: LocalDate = LocalDate.now().minusDays(7),
      toDate: LocalDate = LocalDate.now().minusDays(1),
      applicationDate: LocalDate = LocalDate.now().minusMonths(1),
      submittedAt: LocalDateTime = LocalDateTime.now().minusMonths(1),
      submittedBy: String = "O7h3rU53r",
      approvedAt: LocalDateTime? = null,
      approvedBy: String? = null,
      schedule: JsonNode? = null,
      legacyId: Long? = null,
    ): ((ReferenceDataDomain.Code, String) -> ReferenceData) -> TemporaryAbsenceAuthorisation = { rdSupplier ->
      TemporaryAbsenceAuthorisation(
        personIdentifier,
        prisonCode,
        absenceType?.let { rdSupplier(ABSENCE_TYPE, it) as AbsenceType },
        absenceSubType?.let { rdSupplier(ABSENCE_SUB_TYPE, it) as AbsenceSubType },
        rdSupplier(ABSENCE_REASON, absenceReason) as AbsenceReason,
        repeat,
        rdSupplier(TAP_AUTHORISATION_STATUS, status.name) as TapAuthorisationStatus,
        notes,
        fromDate,
        toDate,
        applicationDate,
        submittedAt,
        submittedBy,
        approvedAt,
        approvedBy,
        schedule,
        legacyId,
      )
    }
  }

  fun TemporaryAbsenceAuthorisation.verifyAgainst(personIdentifier: String, request: TapApplicationRequest) {
    assertThat(this.personIdentifier).isEqualTo(personIdentifier)
    assertThat(legacyId).isEqualTo(request.movementApplicationId)
    assertThat(status.code).isEqualTo(request.tapAuthStatusCode.name)
    assertThat(absenceType?.code).isEqualTo(request.temporaryAbsenceType)
    assertThat(absenceSubType?.code).isEqualTo(request.temporaryAbsenceSubType)
    assertThat(absenceReason?.code).isEqualTo(request.eventSubType)
    assertThat(prisonCode).isEqualTo(request.prisonId)
    assertThat(repeat).isEqualTo(request.isRepeating())
    assertThat(notes).isEqualTo(request.comment)
    assertThat(fromDate).isEqualTo(request.fromDate)
    assertThat(toDate).isEqualTo(request.toDate)
    assertThat(applicationDate).isEqualTo(request.applicationDate)
    assertThat(submittedAt).isCloseTo(request.audit.createDatetime, within(1, SECONDS))
    assertThat(submittedBy).isEqualTo(request.audit.createUsername)
    approvedAt?.also {
      assertThat(it).isCloseTo(request.approvedAt, within(1, SECONDS))
    }
    assertThat(approvedBy).isEqualTo(request.approvedBy)
  }

  fun TemporaryAbsenceAuthorisation.verifyAgainst(personIdentifier: String, request: CreateTapAuthorisationRequest) {
    assertThat(this.personIdentifier).isEqualTo(personIdentifier)
    assertThat(submittedAt).isCloseTo(request.submittedAt, within(1, SECONDS))
    assertThat(status.code).isEqualTo(request.statusCode.name)
    assertThat(absenceType?.code).isEqualTo(request.absenceTypeCode)
    assertThat(absenceSubType?.code).isEqualTo(request.absenceSubTypeCode)
    assertThat(absenceReason?.code).isEqualTo(request.absenceReasonCode)
    assertThat(notes).isEqualTo(request.notes)
    assertThat(repeat).isEqualTo(request.repeat)
    assertThat(legacyId).isNull()
    assertThat(fromDate).isEqualTo(request.fromDate)
    assertThat(toDate).isEqualTo(request.toDate)
    assertThat(applicationDate).isEqualTo(request.applicationDate)
    assertThat(submittedBy).isEqualTo(request.submittedBy)
    approvedAt?.also {
      assertThat(it).isCloseTo(request.approvedAt, within(1, SECONDS))
    }
    assertThat(approvedBy).isEqualTo(request.approvedBy)
    assertThat(schedule).isEqualTo(request.schedule)
  }

  fun TemporaryAbsenceAuthorisation.verifyAgainst(authorisation: TapAuthorisation) {
    assertThat(this.personIdentifier).isEqualTo(personIdentifier)
    assertThat(status.code).isEqualTo(authorisation.status.code)
    assertThat(absenceType?.code).isEqualTo(authorisation.absenceType?.code)
    assertThat(absenceSubType?.code).isEqualTo(authorisation.absenceSubType?.code)
    assertThat(absenceReason?.code).isEqualTo(authorisation.absenceReason?.code)
    assertThat(repeat).isEqualTo(authorisation.repeat)
    assertThat(fromDate).isEqualTo(authorisation.fromDate)
    assertThat(toDate).isEqualTo(authorisation.toDate)
    assertThat(submittedAt).isCloseTo(authorisation.submitted.at, within(1, SECONDS))
    assertThat(submittedBy).isEqualTo(authorisation.submitted.by)
    approvedAt?.also {
      assertThat(it).isCloseTo(authorisation.approved!!.at, within(1, SECONDS))
    }
    assertThat(approvedBy).isEqualTo(authorisation.approved?.by)
    assertThat(schedule).isEqualTo(authorisation.schedule)
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
