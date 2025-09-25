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
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.IdGenerator.newUuid
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.AbsenceReason
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.AbsenceSubType
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.AbsenceType
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.ReferenceData
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.ReferenceDataDomain
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.ReferenceDataDomain.Code.ABSENCE_REASON
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.ReferenceDataDomain.Code.ABSENCE_SUB_TYPE
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.ReferenceDataDomain.Code.ABSENCE_TYPE
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.ReferenceDataDomain.Code.TAP_AUTHORISATION_STATUS
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.TapAuthorisationStatus
import uk.gov.justice.digital.hmpps.externalmovementsapi.exception.NotFoundException
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
  status: TapAuthorisationStatus,
  notes: String?,
  applicationDate: LocalDate,
  submittedAt: LocalDateTime,
  submittedBy: String,
  approvedAt: LocalDateTime?,
  approvedBy: String?,
  legacyId: Long?,
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
}

fun TemporaryAbsenceAuthorisationRepository.getAuthorisation(id: UUID) = findByIdOrNull(id) ?: throw NotFoundException("Temporary absence authorisation not found")
