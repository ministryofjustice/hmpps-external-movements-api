package uk.gov.justice.digital.hmpps.externalmovementsapi.domain

import com.fasterxml.jackson.databind.JsonNode
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.Version
import jakarta.persistence.criteria.JoinType
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.envers.Audited
import org.hibernate.envers.RelationTargetAuditMode.NOT_AUDITED
import org.hibernate.type.SqlTypes
import org.springframework.data.jpa.domain.Specification
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.IdGenerator.newUuid
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.TemporaryAbsenceAuthorisation.Companion.FROM_DATE
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.TemporaryAbsenceAuthorisation.Companion.PERSON_IDENTIFIER
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.TemporaryAbsenceAuthorisation.Companion.PRISON_CODE
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.TemporaryAbsenceAuthorisation.Companion.STATUS
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.TemporaryAbsenceAuthorisation.Companion.TO_DATE
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.interceptor.DomainEventProducer
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.AbsenceReason
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.AbsenceReasonCategory
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.AbsenceSubType
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.AbsenceType
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceData
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceData.Companion.KEY
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_REASON
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_REASON_CATEGORY
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_SUB_TYPE
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_TYPE
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.TAP_AUTHORISATION_STATUS
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataKey.Companion.CODE
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataPaths
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.TapAuthorisationStatus
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.DomainEvent
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.TemporaryAbsenceAuthorised
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
  absenceReasonCategory: AbsenceReasonCategory?,
  absenceReason: AbsenceReason?,
  repeat: Boolean,
  status: TapAuthorisationStatus,
  notes: String?,
  fromDate: LocalDate,
  toDate: LocalDate,
  applicationDate: LocalDate,
  submittedAt: LocalDateTime,
  submittedBy: String,
  approvedAt: LocalDateTime?,
  approvedBy: String?,
  reasonPath: ReasonPath,
  schedule: JsonNode?,
  legacyId: Long?,
  @Id
  @Column(name = "id", nullable = false, updatable = false)
  override val id: UUID = newUuid(),
) : Identifiable,
  DomainEventProducer {
  @Size(max = 10)
  @NotNull
  @Column(name = "person_identifier", nullable = false, length = 10)
  var personIdentifier: String = personIdentifier.uppercase()
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
  @JoinColumn(name = "absence_reason_category_id")
  var absenceReasonCategory: AbsenceReasonCategory? = absenceReasonCategory
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
  @Column(name = "from_date", nullable = false)
  var fromDate: LocalDate = fromDate
    private set

  @NotNull
  @Column(name = "to_date", nullable = false)
  var toDate: LocalDate = toDate
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

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "reason_path")
  var reasonPath: ReasonPath = reasonPath
    private set

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "schedule")
  var schedule: JsonNode? = schedule
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
    rdPaths: ReferenceDataPaths,
  ) = apply {
    this.personIdentifier = personIdentifier.uppercase()
    reasonPath = rdPaths.reasonPath()
    prisonCode = requireNotNull(request.prisonId)
    absenceType = request.temporaryAbsenceType?.let { rdPaths.getReferenceData(ABSENCE_TYPE, it) as AbsenceType }
    absenceSubType = request.temporaryAbsenceSubType?.let { rdPaths.getReferenceData(ABSENCE_SUB_TYPE, it) as AbsenceSubType }
    absenceReasonCategory = reasonPath.path.singleOrNull { it.domain == ABSENCE_REASON_CATEGORY }?.let {
      rdPaths.getReferenceData(it.domain, it.code)
    } as? AbsenceReasonCategory
    absenceReason = rdPaths.getReferenceData(ABSENCE_REASON, request.eventSubType) as AbsenceReason
    repeat = request.isRepeating()
    status =
      rdPaths.getReferenceData(TAP_AUTHORISATION_STATUS, request.tapAuthStatusCode.name) as TapAuthorisationStatus
    notes = request.comment
    submittedAt = request.audit.createDatetime
    submittedBy = request.audit.createUsername
    legacyId = request.movementApplicationId
    applicationDate = request.applicationDate
    approvedAt = request.approvedAt
    approvedBy = request.approvedBy
    fromDate = request.fromDate
    toDate = request.toDate
  }

  override fun initialEvent(): DomainEvent<*>? = if (status.code == TapAuthorisationStatus.Code.APPROVED.name) {
    TemporaryAbsenceAuthorised(personIdentifier, id)
  } else {
    null
  }

  override fun stateChangedEvent(previousState: (String) -> Any?): DomainEvent<*>? {
    val previousStatus = previousState(STATUS) as TapAuthorisationStatus
    return if (previousStatus.code != status.code && status.code == TapAuthorisationStatus.Code.APPROVED.name) {
      TemporaryAbsenceAuthorised(personIdentifier, id)
    } else {
      null
    }
  }

  companion object {
    val PRISON_CODE = TemporaryAbsenceAuthorisation::prisonCode.name
    val PERSON_IDENTIFIER = TemporaryAbsenceAuthorisation::personIdentifier.name
    val FROM_DATE = TemporaryAbsenceAuthorisation::fromDate.name
    val TO_DATE = TemporaryAbsenceAuthorisation::toDate.name
    val STATUS = TemporaryAbsenceAuthorisation::status.name
  }
}

interface TemporaryAbsenceAuthorisationRepository :
  JpaRepository<TemporaryAbsenceAuthorisation, UUID>,
  JpaSpecificationExecutor<TemporaryAbsenceAuthorisation> {
  fun findByLegacyId(legacyId: Long): TemporaryAbsenceAuthorisation?

  @Query(
    """
    select count(1) as approvalsRequired
    from TemporaryAbsenceAuthorisation taa
    where taa.prisonCode = :prisonIdentifier 
    and taa.status.key.code = 'PENDING' and taa.fromDate >= current_date
  """,
  )
  fun findApprovalsRequiredCount(prisonIdentifier: String): Int
}

fun TemporaryAbsenceAuthorisationRepository.getAuthorisation(id: UUID) = findByIdOrNull(id) ?: throw NotFoundException("Temporary absence authorisation not found")

fun authorisationMatchesPrisonCode(prisonCode: String) = Specification<TemporaryAbsenceAuthorisation> { taa, _, cb ->
  cb.equal(taa.get<String>(PRISON_CODE), prisonCode)
}

fun authorisationMatchesPersonIdentifier(personIdentifier: String) = Specification<TemporaryAbsenceAuthorisation> { taa, _, cb ->
  cb.equal(taa.get<String>(PERSON_IDENTIFIER), personIdentifier)
}

fun authorisationMatchesDateRange(fromDate: LocalDate, toDate: LocalDate) = Specification<TemporaryAbsenceAuthorisation> { taa, _, cb ->
  cb.and(cb.greaterThanOrEqualTo(taa.get(FROM_DATE), fromDate), cb.lessThanOrEqualTo(taa.get(TO_DATE), toDate))
}

fun authorisationStatusCodeIn(statusCodes: Set<TapAuthorisationStatus.Code>) = Specification<TemporaryAbsenceAuthorisation> { taa, _, _ ->
  val status = taa.join<TemporaryAbsenceAuthorisation, ReferenceData>(STATUS, JoinType.INNER)
  status.get<String>(KEY).get<String>(CODE).`in`(statusCodes.map { it.name })
}
