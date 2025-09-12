package uk.gov.justice.digital.hmpps.externalmovementsapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.springframework.data.repository.findByIdOrNull
import org.springframework.transaction.support.TransactionTemplate
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.TemporaryAbsenceSeries
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.TemporaryAbsenceSeriesRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.AbsenceReason
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.AbsenceSubType
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.AbsenceType
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.AccompaniedBy
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.LocationType
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.ReferenceData
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.ReferenceDataDomain
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.ReferenceDataDomain.Code.ABSENCE_REASON
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.ReferenceDataDomain.Code.ABSENCE_SUB_TYPE
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.ReferenceDataDomain.Code.ABSENCE_TYPE
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.ReferenceDataDomain.Code.ACCOMPANIED_BY
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.ReferenceDataDomain.Code.LOCATION_TYPE
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.ReferenceDataDomain.Code.TAP_STATUS
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.ReferenceDataDomain.Code.TRANSPORT
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.ReferenceDataRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.TapStatus
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.Transport
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.of
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.DataGenerator.personIdentifier
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.tapseries.CreateTapSeriesRequest
import uk.gov.justice.digital.hmpps.externalmovementsapi.sync.TapApplicationRequest
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit.SECONDS
import java.util.UUID

interface TempAbsenceSeriesOperations {
  fun givenTemporaryAbsenceSeries(tas: ((ReferenceDataDomain.Code, String) -> ReferenceData) -> TemporaryAbsenceSeries): TemporaryAbsenceSeries
  fun findTemporaryAbsenceSeries(id: UUID): TemporaryAbsenceSeries?

  companion object {
    fun temporaryAbsenceSeries(
      prisonCode: String,
      personIdentifier: String = personIdentifier(),
      status: String = "APP-SCH",
      absenceType: String? = "SR",
      absenceSubType: String? = "RDR",
      absenceReason: String = "R15",
      repeat: Boolean = false,
      releaseAt: LocalDateTime = LocalDateTime.now().minusDays(7),
      returnBy: LocalDateTime = LocalDateTime.now(),
      locationType: String = "OTHER",
      locationId: String? = null,
      accompanied: Boolean = true,
      accompaniedBy: String? = "L",
      transport: String? = "OD",
      notes: String? = "Some notes on the original creation",
      submittedAt: LocalDateTime = LocalDateTime.now().minusMonths(1),
      legacyId: Long? = null,
      toAgencyCode: String? = null,
    ): ((ReferenceDataDomain.Code, String) -> ReferenceData) -> TemporaryAbsenceSeries = { rdSupplier ->
      TemporaryAbsenceSeries(
        personIdentifier,
        prisonCode,
        absenceType?.let { rdSupplier(ABSENCE_TYPE, it) as AbsenceType },
        absenceSubType?.let { rdSupplier(ABSENCE_SUB_TYPE, it) as AbsenceSubType },
        rdSupplier(ABSENCE_REASON, absenceReason) as AbsenceReason,
        repeat,
        releaseAt,
        returnBy,
        rdSupplier(LOCATION_TYPE, locationType) as LocationType,
        locationId,
        accompanied,
        accompaniedBy?.let { rdSupplier(ACCOMPANIED_BY, it) as AccompaniedBy },
        transport?.let { rdSupplier(TRANSPORT, it) as Transport },
        rdSupplier(TAP_STATUS, status) as TapStatus,
        notes,
        submittedAt,
        legacyId,
        toAgencyCode,
      )
    }
  }

  fun TemporaryAbsenceSeries.verifyAgainst(personIdentifier: String, request: TapApplicationRequest) {
    assertThat(this.personIdentifier).isEqualTo(personIdentifier)
    assertThat(legacyId).isEqualTo(request.movementApplicationId)
    assertThat(status.code).isEqualTo(request.applicationStatus)
    assertThat(absenceType?.code).isEqualTo(request.temporaryAbsenceType)
    assertThat(absenceSubType?.code).isEqualTo(request.temporaryAbsenceSubType)
    assertThat(absenceReason?.code).isEqualTo(request.eventSubType)
    assertThat(prisonCode).isEqualTo(request.prisonId)
    assertThat(repeat).isEqualTo(request.isRepeating())
    assertThat(releaseAt.truncatedTo(SECONDS)).isEqualTo(request.releaseTime.truncatedTo(SECONDS))
    assertThat(returnBy.truncatedTo(SECONDS)).isEqualTo(request.returnTime.truncatedTo(SECONDS))
    assertThat(locationType.code).isEqualTo(request.toAddressOwnerClass ?: "OTHER")
    assertThat(locationId).isEqualTo(request.toAddressId?.toString())
    assertThat(accompanied).isEqualTo(request.isAccompanied())
    assertThat(accompaniedBy?.code).isEqualTo(request.escortCode)
    assertThat(transport?.code).isEqualTo(request.transportType)
    assertThat(notes).isEqualTo(request.comment)
    assertThat(submittedAt.truncatedTo(SECONDS)).isEqualTo(
      LocalDateTime.of(request.applicationDate, LocalTime.MIN).truncatedTo(SECONDS),
    )
    assertThat(toAgencyCode).isEqualTo(request.toAgencyId)
  }

  fun TemporaryAbsenceSeries.verifyAgainst(personIdentifier: String, request: CreateTapSeriesRequest) {
    assertThat(this.personIdentifier).isEqualTo(personIdentifier)
    assertThat(submittedAt.truncatedTo(SECONDS)).isEqualTo(submittedAt.truncatedTo(SECONDS))
    assertThat(status.code).isEqualTo(request.statusCode)
    assertThat(absenceType?.code).isEqualTo(request.absenceTypeCode)
    assertThat(absenceSubType?.code).isEqualTo(request.absenceSubTypeCode)
    assertThat(absenceReason?.code).isEqualTo(request.absenceReasonCode)
    assertThat(repeat).isEqualTo(request.repeat)
    assertThat(releaseAt.truncatedTo(SECONDS)).isEqualTo(request.releaseAt.truncatedTo(SECONDS))
    assertThat(returnBy.truncatedTo(SECONDS)).isEqualTo(request.returnBy.truncatedTo(SECONDS))
    assertThat(accompanied).isEqualTo(request.accompanied)
    assertThat(accompaniedBy?.code).isEqualTo(request.accompaniedByCode)
    assertThat(transport?.code).isEqualTo(request.transportCode)
    assertThat(notes).isEqualTo(request.notes)
    assertThat(locationType.code).isEqualTo(request.locationTypeCode)
    assertThat(locationId).isEqualTo(request.locationId)
    assertThat(legacyId).isNull()
    assertThat(toAgencyCode).isNull()
  }
}

class TempAbsenceSeriesOperationsImpl(
  private val transactionTemplate: TransactionTemplate,
  private val referenceDataRepository: ReferenceDataRepository,
  private val temporaryAbsenceSeriesRepository: TemporaryAbsenceSeriesRepository,
) : TempAbsenceSeriesOperations {
  override fun givenTemporaryAbsenceSeries(tas: ((ReferenceDataDomain.Code, String) -> ReferenceData) -> TemporaryAbsenceSeries): TemporaryAbsenceSeries = transactionTemplate.execute {
    val rdMap = referenceDataRepository.findAll().associateBy { it.key.domain of it.key.code }
    val series: TemporaryAbsenceSeries = tas { dc: ReferenceDataDomain.Code, c: String ->
      requireNotNull(rdMap[dc of c])
    }
    temporaryAbsenceSeriesRepository.save(series)
  }!!

  override fun findTemporaryAbsenceSeries(id: UUID): TemporaryAbsenceSeries? = temporaryAbsenceSeriesRepository.findByIdOrNull(id)
}
