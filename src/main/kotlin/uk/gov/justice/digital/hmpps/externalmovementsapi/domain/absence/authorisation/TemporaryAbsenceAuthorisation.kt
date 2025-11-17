package uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.authorisation

import com.fasterxml.jackson.databind.JsonNode
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.PostLoad
import jakarta.persistence.Table
import jakarta.persistence.Transient
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
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.Identifiable
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.ReasonPath
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.AbsenceCategorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.authorisation.TemporaryAbsenceAuthorisation.Companion.FROM_DATE
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.authorisation.TemporaryAbsenceAuthorisation.Companion.PERSON_IDENTIFIER
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.authorisation.TemporaryAbsenceAuthorisation.Companion.PRISON_CODE
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.authorisation.TemporaryAbsenceAuthorisation.Companion.STATUS
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.authorisation.TemporaryAbsenceAuthorisation.Companion.TO_DATE
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.interceptor.DomainEventProducer
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.AbsenceReason
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.AbsenceReasonCategory
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.AbsenceSubType
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.AbsenceType
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.AccompaniedBy
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceData
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceData.Companion.KEY
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_REASON
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_REASON_CATEGORY
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_SUB_TYPE
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_TYPE
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.TAP_AUTHORISATION_STATUS
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataKey.Companion.CODE
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.TapAuthorisationStatus
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.DomainEvent
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.TemporaryAbsenceAuthorisationApproved
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.TemporaryAbsenceAuthorisationPending
import uk.gov.justice.digital.hmpps.externalmovementsapi.exception.NotFoundException
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.authorisation.AmendAuthorisationNotes
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.authorisation.ApproveAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.authorisation.AuthorisationAction
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.authorisation.CancelAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.authorisation.ChangeAbsenceCategorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.authorisation.ChangeAuthorisationCase
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.authorisation.DenyAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.authorisation.RescheduleAuthorisation
import java.time.LocalDate
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
  accompaniedBy: AccompaniedBy,
  repeat: Boolean,
  status: TapAuthorisationStatus,
  notes: String?,
  fromDate: LocalDate,
  toDate: LocalDate,
  reasonPath: ReasonPath,
  schedule: JsonNode?,
  legacyId: Long?,
  @Id
  @Column(name = "id", nullable = false, updatable = false)
  override val id: UUID = newUuid(),
) : Identifiable,
  AbsenceCategorisation,
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
  @ManyToOne
  @JoinColumn(name = "absence_type_id")
  override var absenceType: AbsenceType? = absenceType
    private set

  @Audited(targetAuditMode = NOT_AUDITED)
  @ManyToOne
  @JoinColumn(name = "absence_sub_type_id")
  override var absenceSubType: AbsenceSubType? = absenceSubType
    private set

  @Audited(targetAuditMode = NOT_AUDITED)
  @ManyToOne
  @JoinColumn(name = "absence_reason_category_id")
  override var absenceReasonCategory: AbsenceReasonCategory? = absenceReasonCategory
    private set

  @Audited(targetAuditMode = NOT_AUDITED)
  @ManyToOne
  @JoinColumn(name = "absence_reason_id")
  override var absenceReason: AbsenceReason? = absenceReason
    private set

  @Audited(targetAuditMode = NOT_AUDITED)
  @ManyToOne(optional = false)
  @JoinColumn(name = "accompanied_by_id", nullable = false)
  var accompaniedBy: AccompaniedBy = accompaniedBy
    private set

  @NotNull
  @Column(name = "repeat", nullable = false)
  var repeat: Boolean = repeat
    private set

  @Audited(targetAuditMode = NOT_AUDITED)
  @ManyToOne(optional = false)
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

  override fun initialEvent(): DomainEvent<*>? = if (status.code == TapAuthorisationStatus.Code.APPROVED.name) {
    TemporaryAbsenceAuthorisationApproved(personIdentifier, id)
  } else {
    TemporaryAbsenceAuthorisationPending(personIdentifier, id)
  }

  @Transient
  private var appliedActions: List<AuthorisationAction> = listOf()

  @PostLoad
  private fun load() {
    appliedActions = listOf()
  }

  override fun domainEvents(): Set<DomainEvent<*>> = appliedActions.mapNotNull { it.domainEvent(this) }.toSet()

  fun applyPrisonPerson(action: ChangeAuthorisationCase) {
    personIdentifier = action.personIdentifier.uppercase()
    prisonCode = action.prisonCode
  }

  fun applyAbsenceCategorisation(action: ChangeAbsenceCategorisation, rdSupplier: (ReferenceDataDomain.Code, String) -> ReferenceData) {
    if (action.changes(this)) {
      reasonPath = action.reasonPath
      absenceReason = action.absenceReasonCode?.let { rdSupplier(ABSENCE_REASON, it) as AbsenceReason }
      absenceReasonCategory =
        action.absenceReasonCategoryCode?.let { rdSupplier(ABSENCE_REASON_CATEGORY, it) as AbsenceReasonCategory }
      absenceSubType = action.absenceSubTypeCode?.let { rdSupplier(ABSENCE_SUB_TYPE, it) as AbsenceSubType }
      absenceType = action.absenceTypeCode?.let { rdSupplier(ABSENCE_TYPE, it) as AbsenceType }
      appliedActions += action
    }
  }

  fun reschedule(action: RescheduleAuthorisation) {
    repeat = action.repeat
    fromDate = action.from
    toDate = action.to
  }

  fun amendNotes(action: AmendAuthorisationNotes) {
    if (action.changes(notes)) {
      notes = action.notes
      appliedActions += action
    }
  }

  fun approve(action: ApproveAuthorisation, rdSupplier: (ReferenceDataDomain.Code, String) -> ReferenceData) {
    applyStatus(TapAuthorisationStatus.Code.APPROVED, rdSupplier, action)
  }

  fun deny(action: DenyAuthorisation, rdSupplier: (ReferenceDataDomain.Code, String) -> ReferenceData) {
    applyStatus(TapAuthorisationStatus.Code.DENIED, rdSupplier, action)
  }

  fun cancel(action: CancelAuthorisation, rdSupplier: (ReferenceDataDomain.Code, String) -> ReferenceData) {
    applyStatus(TapAuthorisationStatus.Code.CANCELLED, rdSupplier, action)
  }

  private fun applyStatus(
    statusCode: TapAuthorisationStatus.Code,
    rdSupplier: (ReferenceDataDomain.Code, String) -> ReferenceData,
    action: AuthorisationAction,
  ) {
    if (status.code != statusCode.name) {
      status = rdSupplier(TAP_AUTHORISATION_STATUS, statusCode.name) as TapAuthorisationStatus
      appliedActions += action
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
