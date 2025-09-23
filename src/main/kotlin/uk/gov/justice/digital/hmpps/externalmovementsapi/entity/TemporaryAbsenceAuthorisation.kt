package uk.gov.justice.digital.hmpps.externalmovementsapi.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.Version
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import org.hibernate.envers.Audited
import org.hibernate.envers.RelationTargetAuditMode.NOT_AUDITED
import org.springframework.data.jpa.repository.JpaRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.IdGenerator.newUuid
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
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.ReferenceDataDomain.Code.TAP_AUTHORISATION_STATUS
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.ReferenceDataDomain.Code.TRANSPORT
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.TapAuthorisationStatus
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.Transport
import uk.gov.justice.digital.hmpps.externalmovementsapi.sync.TapApplicationRequest
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Audited
@Entity
@Table(name = "temporary_absence_authorisation")
class TemporaryAbsenceAuthorisation(
  personIdentifier: String,
  prisonCode: String,
  absenceType: AbsenceType?,
  absenceSubType: AbsenceSubType?,
  absenceReason: AbsenceReason?,
  repeat: Boolean,
  releaseAt: LocalDateTime,
  returnBy: LocalDateTime,
  locationType: LocationType,
  locationId: String?,
  accompanied: Boolean,
  accompaniedBy: AccompaniedBy?,
  transport: Transport?,
  status: TapAuthorisationStatus,
  notes: String?,
  applicationDate: LocalDate,
  submittedAt: LocalDateTime,
  submittedBy: String,
  approvedAt: LocalDateTime?,
  approvedBy: String?,
  legacyId: Long?,
  contact: String?,
  @Id
  @Column(name = "id", nullable = false)
  val id: UUID = newUuid(),
) {
  @Size(max = 10)
  @NotNull
  @Column(name = "person_identifier", nullable = false, length = 10)
  var personIdentifier: String = personIdentifier
    private set

  @Size(max = 6)
  @NotNull
  @Column(name = "prison_code", nullable = false, length = 6)
  var prisonCode: String = prisonCode
    private set

  @Audited(targetAuditMode = NOT_AUDITED)
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "absence_type_id")
  var absenceType: AbsenceType? = absenceType
    private set

  @Audited(targetAuditMode = NOT_AUDITED)
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "absence_sub_type_id")
  var absenceSubType: AbsenceSubType? = absenceSubType
    private set

  @Audited(targetAuditMode = NOT_AUDITED)
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "absence_reason_id")
  var absenceReason: AbsenceReason? = absenceReason
    private set

  @NotNull
  @Column(name = "repeat", nullable = false)
  var repeat: Boolean = repeat
    private set

  @NotNull
  @Column(name = "release_at", nullable = false)
  var releaseAt: LocalDateTime = releaseAt
    private set

  @NotNull
  @Column(name = "return_by", nullable = false)
  var returnBy: LocalDateTime = returnBy
    private set

  @Audited(targetAuditMode = NOT_AUDITED)
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "location_type_id")
  var locationType: LocationType = locationType
    private set

  @Size(max = 36)
  @Column(name = "location_id", length = 36)
  var locationId: String? = locationId
    private set

  @NotNull
  @Column(name = "accompanied", nullable = false)
  var accompanied: Boolean = accompanied
    private set

  @Audited(targetAuditMode = NOT_AUDITED)
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "accompanied_by_id")
  var accompaniedBy: AccompaniedBy? = accompaniedBy
    private set

  @Audited(targetAuditMode = NOT_AUDITED)
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "transport_id")
  var transport: Transport? = transport
    private set

  @Audited(targetAuditMode = NOT_AUDITED)
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "status_id", nullable = false)
  var status: TapAuthorisationStatus = status
    private set

  @Column(name = "notes")
  var notes: String? = notes
    private set

  @NotNull
  @Column(name = "application_date", nullable = false)
  var applicationDate: LocalDate = applicationDate
    private set

  @NotNull
  @Column(name = "submitted_at", nullable = false)
  var submittedAt: LocalDateTime = submittedAt
    private set

  @NotNull
  @Column(name = "submitted_by", nullable = false)
  var submittedBy: String = submittedBy
    private set

  @Column(name = "approved_at")
  var approvedAt: LocalDateTime? = approvedAt
    private set

  @Column(name = "approved_by", nullable = false)
  var approvedBy: String? = approvedBy
    private set

  @Column(name = "legacy_id")
  var legacyId: Long? = legacyId
    private set

  @Column(name = "contact")
  var contact: String? = contact
    private set

  @Version
  var version: Int? = null
    private set

  fun update(
    personIdentifier: String,
    request: TapApplicationRequest,
    rdProvider: (ReferenceDataDomain.Code, String) -> ReferenceData,
  ) = apply {
    this.personIdentifier = personIdentifier
    prisonCode = requireNotNull(request.prisonId)
    absenceType = request.temporaryAbsenceType?.let { rdProvider(ABSENCE_TYPE, it) as AbsenceType }
    absenceSubType = request.temporaryAbsenceSubType?.let { rdProvider(ABSENCE_SUB_TYPE, it) as AbsenceSubType }
    absenceReason = rdProvider(ABSENCE_REASON, request.eventSubType) as AbsenceReason
    repeat = request.isRepeating()
    releaseAt = request.releaseTime
    returnBy = request.returnTime
    locationType =
      request.toAddressOwnerClass?.let { rdProvider(ReferenceDataDomain.Code.LOCATION_TYPE, it) as? LocationType }
        ?: rdProvider(ReferenceDataDomain.Code.LOCATION_TYPE, "OTHER") as LocationType
    locationId = request.toAddressId?.toString()
    accompanied = request.isAccompanied()
    accompaniedBy = request.escortCode?.let { rdProvider(ACCOMPANIED_BY, it) as AccompaniedBy }
    transport = request.transportType?.let { rdProvider(TRANSPORT, it) as Transport }
    status = rdProvider(TAP_AUTHORISATION_STATUS, request.tapAuthStatusCode.name) as TapAuthorisationStatus
    notes = request.comment
    submittedAt = request.audit.createDatetime
    submittedBy = request.audit.createUsername
    legacyId = request.movementApplicationId
    applicationDate = request.applicationDate
    approvedAt = request.approvedAt
    approvedBy = request.approvedBy
  }
}

interface TemporaryAbsenceAuthorisationRepository : JpaRepository<TemporaryAbsenceAuthorisation, UUID> {
  fun findByLegacyId(legacyId: Long): TemporaryAbsenceAuthorisation?
  fun findByPersonIdentifierAndReleaseAtAndReturnBy(
    personIdentifier: String,
    releaseAt: LocalDateTime,
    returnBy: LocalDateTime,
  ): TemporaryAbsenceAuthorisation?
}
