package uk.gov.justice.digital.hmpps.externalmovementsapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.springframework.data.repository.findByIdOrNull
import org.springframework.transaction.support.TransactionTemplate
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.TemporaryAbsenceAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.TemporaryAbsenceAuthorisationRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.AbsenceReason
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.AbsenceSubType
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.AbsenceType
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.ReferenceData
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.ReferenceDataDomain
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.ReferenceDataDomain.Code.ABSENCE_REASON
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.ReferenceDataDomain.Code.ABSENCE_SUB_TYPE
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.ReferenceDataDomain.Code.ABSENCE_TYPE
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.ReferenceDataDomain.Code.TAP_AUTHORISATION_STATUS
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.ReferenceDataRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.TapAuthorisationStatus
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.of
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.DataGenerator.personIdentifier
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.CreateTapAuthorisationRequest
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
      prisonCode: String,
      personIdentifier: String = personIdentifier(),
      status: TapAuthorisationStatus.Code = TapAuthorisationStatus.Code.APPROVED,
      absenceType: String? = "SR",
      absenceSubType: String? = "RDR",
      absenceReason: String = "R15",
      repeat: Boolean = false,
      notes: String? = "Some notes on the original authorisation",
      applicationDate: LocalDate = LocalDate.now().minusMonths(1),
      submittedAt: LocalDateTime = LocalDateTime.now().minusMonths(1),
      submittedBy: String = "O7h3rU53r",
      approvedAt: LocalDateTime? = null,
      approvedBy: String? = null,
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
        applicationDate,
        submittedAt,
        submittedBy,
        approvedAt,
        approvedBy,
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
    assertThat(legacyId).isNull()
    assertThat(applicationDate).isEqualTo(request.applicationDate)
    assertThat(submittedBy).isEqualTo(request.submittedBy)
    approvedAt?.also {
      assertThat(it).isCloseTo(request.approvedAt, within(1, SECONDS))
    }
    assertThat(approvedBy).isEqualTo(request.approvedBy)
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
