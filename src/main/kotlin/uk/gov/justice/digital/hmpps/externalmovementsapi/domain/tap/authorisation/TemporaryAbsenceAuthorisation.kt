package uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.authorisation

import com.fasterxml.jackson.databind.JsonNode
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.PostLoad
import jakarta.persistence.QueryHint
import jakarta.persistence.Table
import jakarta.persistence.Transient
import jakarta.persistence.Version
import jakarta.persistence.criteria.JoinType
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.envers.Audited
import org.hibernate.envers.RelationTargetAuditMode.NOT_AUDITED
import org.hibernate.jpa.HibernateHints
import org.hibernate.type.SqlTypes
import org.springframework.data.jpa.domain.Specification
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.jpa.repository.QueryHints
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.IdGenerator.newUuid
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.Identifiable
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.ReasonPath
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.interceptor.DomainEventProducer
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.person.PersonSummary
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.person.matchesIdentifier
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.person.matchesName
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceData
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceData.Companion.CODE
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.CategorisedAbsenceReason
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.authorisation.TemporaryAbsenceAuthorisation.Companion.END
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.authorisation.TemporaryAbsenceAuthorisation.Companion.PERSON
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.authorisation.TemporaryAbsenceAuthorisation.Companion.PRISON_CODE
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.authorisation.TemporaryAbsenceAuthorisation.Companion.START
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.authorisation.TemporaryAbsenceAuthorisation.Companion.STATUS
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.AccompaniedBy
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.AuthorisationStatus
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.AuthorisationStatus.Code.APPROVED
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.AuthorisationStatus.Code.CANCELLED
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.AuthorisationStatus.Code.DENIED
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.AuthorisationStatus.Code.EXPIRED
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.AuthorisationStatus.Code.PENDING
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.Transport
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.absencereason.AbsenceReason
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.absencereason.AbsenceReasonCategory
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.absencereason.AbsenceSubType
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.absencereason.AbsenceType
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.DomainEvent
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.TemporaryAbsenceAuthorisationApproved
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.TemporaryAbsenceAuthorisationPending
import uk.gov.justice.digital.hmpps.externalmovementsapi.exception.NotFoundException
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.authorisation.ApproveAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.authorisation.AuthorisationAction
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.authorisation.CancelAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.authorisation.ChangeAuthorisationAccompaniment
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.authorisation.ChangeAuthorisationComments
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.authorisation.ChangeAuthorisationDateRange
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.authorisation.ChangeAuthorisationTransport
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.authorisation.ChangePrisonPerson
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.authorisation.DeferAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.authorisation.DenyAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.authorisation.ExpireAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.authorisation.RecategoriseAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.paged.AbsenceCategorisationFilter
import java.time.LocalDate
import java.time.LocalDate.now
import java.util.UUID
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

@Audited
@Entity
@Table(schema = "tap", name = "authorisation")
class TemporaryAbsenceAuthorisation(
  person: PersonSummary,
  prisonCode: String,
  status: AuthorisationStatus,
  absenceType: AbsenceType?,
  absenceSubType: AbsenceSubType?,
  absenceReasonCategory: AbsenceReasonCategory?,
  absenceReason: AbsenceReason,
  accompaniedBy: AccompaniedBy,
  transport: Transport,
  repeat: Boolean,
  comments: String?,
  start: LocalDate,
  end: LocalDate,
  reasonPath: ReasonPath,
  schedule: JsonNode?,
  legacyId: Long?,
  @Id
  @Column(name = "id", nullable = false, updatable = false)
  override val id: UUID = newUuid(),
) : Identifiable,
  CategorisedAbsenceReason,
  DomainEventProducer {

  @Audited(targetAuditMode = NOT_AUDITED)
  @ManyToOne
  @JoinColumn(name = "person_identifier")
  var person: PersonSummary = person
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
  override var absenceReason: AbsenceReason = absenceReason
    private set

  @Audited(targetAuditMode = NOT_AUDITED)
  @ManyToOne(optional = false)
  @JoinColumn(name = "accompanied_by_id", nullable = false)
  var accompaniedBy: AccompaniedBy = accompaniedBy
    private set

  @Audited(targetAuditMode = NOT_AUDITED)
  @ManyToOne(optional = false)
  @JoinColumn(name = "transport_id", nullable = false)
  var transport: Transport = transport
    private set

  @NotNull
  @Column(name = "repeat", nullable = false)
  var repeat: Boolean = repeat
    private set

  @Audited(targetAuditMode = NOT_AUDITED)
  @ManyToOne(optional = false)
  @JoinColumn(name = "status_id", nullable = false)
  var status: AuthorisationStatus = status
    private set

  @Column(name = "comments")
  var comments: String? = comments
    private set

  @NotNull
  @Column(name = "start", nullable = false)
  var start: LocalDate = start
    private set

  @NotNull
  @Column(name = "end", nullable = false)
  var end: LocalDate = end
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
  override var version: Int? = null
    private set

  override fun initialEvent(): DomainEvent<*>? = if (status.code == APPROVED.name) {
    TemporaryAbsenceAuthorisationApproved(person.identifier, id)
  } else {
    TemporaryAbsenceAuthorisationPending(person.identifier, id)
  }

  @Transient
  private var appliedActions: List<AuthorisationAction> = listOf()

  @PostLoad
  private fun load() {
    appliedActions = listOf()
  }

  override fun domainEvents(): Set<DomainEvent<*>> = appliedActions.mapNotNull { it.domainEvent(this) }.toSet()

  fun applyPrisonPerson(action: ChangePrisonPerson, person: (String) -> PersonSummary) {
    this.person = person(action.personIdentifier)
    prisonCode = action.prisonCode
  }

  fun moveTo(person: PersonSummary) = apply {
    this.person = person
  }

  fun applyAbsenceCategorisation(
    action: RecategoriseAuthorisation,
    rdSupplier: (KClass<out ReferenceData>, String) -> ReferenceData,
  ) {
    if (action.changes(this)) {
      reasonPath = action.reasonPath
      absenceReason = rdSupplier(AbsenceReason::class, requireNotNull(action.absenceReasonCode)) as AbsenceReason
      absenceReasonCategory = action.absenceReasonCategoryCode?.let {
        rdSupplier(AbsenceReasonCategory::class, it) as AbsenceReasonCategory
      }
      absenceSubType = action.absenceSubTypeCode?.let { rdSupplier(AbsenceSubType::class, it) as AbsenceSubType }
      absenceType = action.absenceTypeCode?.let { rdSupplier(AbsenceType::class, it) as AbsenceType }
      appliedActions += action
    }
  }

  fun applyDateRange(
    action: ChangeAuthorisationDateRange,
    rdSupplier: (KClass<out ReferenceData>, String) -> ReferenceData,
  ) {
    if (!start.isEqual(action.start) || !end.isEqual(action.end)) {
      start = action.start
      end = action.end
      appliedActions += action
      if (end.isBefore(now())) {
        expire(ExpireAuthorisation(), rdSupplier)
      }
    }
  }

  fun applyComments(action: ChangeAuthorisationComments) {
    if (action changes comments) {
      comments = action.comments
      appliedActions += action
    }
  }

  fun applyAccompaniment(
    action: ChangeAuthorisationAccompaniment,
    rdSupplier: (KClass<out ReferenceData>, String) -> ReferenceData,
  ) {
    if (action.accompaniedByCode != accompaniedBy.code) {
      accompaniedBy = rdSupplier(AccompaniedBy::class, action.accompaniedByCode) as AccompaniedBy
      appliedActions += action
    }
  }

  fun applyTransport(
    action: ChangeAuthorisationTransport,
    rdSupplier: (KClass<out ReferenceData>, String) -> ReferenceData,
  ) {
    if (action.transportCode != transport.code) {
      transport = rdSupplier(Transport::class, action.transportCode) as Transport
      appliedActions += action
    }
  }

  fun defer(action: DeferAuthorisation, rdSupplier: (KClass<out ReferenceData>, String) -> ReferenceData) {
    applyStatus(PENDING, rdSupplier, action)
  }

  fun approve(action: ApproveAuthorisation, rdSupplier: (KClass<out ReferenceData>, String) -> ReferenceData) {
    applyStatus(APPROVED, rdSupplier, action)
  }

  fun deny(action: DenyAuthorisation, rdSupplier: (KClass<out ReferenceData>, String) -> ReferenceData) {
    applyStatus(DENIED, rdSupplier, action)
  }

  fun cancel(action: CancelAuthorisation, rdSupplier: (KClass<out ReferenceData>, String) -> ReferenceData) {
    applyStatus(CANCELLED, rdSupplier, action)
  }

  fun expire(action: ExpireAuthorisation, rdSupplier: (KClass<out ReferenceData>, String) -> ReferenceData) {
    if (status.code == PENDING.name) {
      applyStatus(EXPIRED, rdSupplier, action)
    }
  }

  fun permitsOccurrences(): Boolean = status.code in listOf(PENDING.name, APPROVED.name)

  private fun applyStatus(
    statusCode: AuthorisationStatus.Code,
    rdSupplier: (KClass<out ReferenceData>, String) -> ReferenceData,
    action: AuthorisationAction,
  ) {
    if (status.code != statusCode.name) {
      status = rdSupplier(AuthorisationStatus::class, statusCode.name) as AuthorisationStatus
      appliedActions += action
    }
  }

  companion object {
    val PRISON_CODE = TemporaryAbsenceAuthorisation::prisonCode.name
    val PERSON = TemporaryAbsenceAuthorisation::person.name
    val START = TemporaryAbsenceAuthorisation::start.name
    val END = TemporaryAbsenceAuthorisation::end.name
    val STATUS = TemporaryAbsenceAuthorisation::status.name
    val REPEAT = TemporaryAbsenceAuthorisation::repeat.name
    val ABSENCE_TYPE = TemporaryAbsenceAuthorisation::absenceType.name
    val ABSENCE_SUB_TYPE = TemporaryAbsenceAuthorisation::absenceSubType.name
    val ABSENCE_REASON_CATEGORY = TemporaryAbsenceAuthorisation::absenceReasonCategory.name
    val ABSENCE_REASON = TemporaryAbsenceAuthorisation::absenceReason.name
    val ID = TemporaryAbsenceAuthorisation::id.name

    fun changeableProperties(): Set<KProperty1<TemporaryAbsenceAuthorisation, Any?>> = setOf(
      TemporaryAbsenceAuthorisation::absenceType,
      TemporaryAbsenceAuthorisation::absenceSubType,
      TemporaryAbsenceAuthorisation::absenceReasonCategory,
      TemporaryAbsenceAuthorisation::absenceReason,
      TemporaryAbsenceAuthorisation::start,
      TemporaryAbsenceAuthorisation::end,
      TemporaryAbsenceAuthorisation::accompaniedBy,
      TemporaryAbsenceAuthorisation::transport,
      TemporaryAbsenceAuthorisation::status,
      TemporaryAbsenceAuthorisation::comments,
    )
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
    and taa.status.code = 'PENDING' and taa.end >= current_date
  """,
  )
  fun findApprovalsRequiredCount(prisonIdentifier: String): Int

  @Query(
    """
      select taa from TemporaryAbsenceAuthorisation taa
      where taa.status.code = 'PENDING' and taa.end < current_date
    """,
  )
  @QueryHints(value = [QueryHint(name = HibernateHints.HINT_NATIVE_LOCK_MODE, value = "UPGRADE-SKIPLOCKED")])
  fun findRecentlyExpired(): List<TemporaryAbsenceAuthorisation>

  fun countByPersonIdentifier(personIdentifier: String): Int

  @Modifying
  @Query("delete from TemporaryAbsenceAuthorisation taa where taa.person.identifier = :personIdentifier")
  fun deleteByPersonIdentifier(personIdentifier: String)
}

fun TemporaryAbsenceAuthorisationRepository.getAuthorisation(id: UUID) = findByIdOrNull(id) ?: throw NotFoundException("Temporary absence authorisation not found")

fun authorisationMatchesPrisonCode(prisonCode: String) = Specification<TemporaryAbsenceAuthorisation> { taa, _, cb ->
  cb.equal(taa.get<String>(PRISON_CODE), prisonCode)
}

fun authorisationMatchesPersonIdentifier(personIdentifier: String) = Specification<TemporaryAbsenceAuthorisation> { taa, _, cb ->
  taa.join<TemporaryAbsenceAuthorisation, PersonSummary>(PERSON, JoinType.INNER)
    .matchesIdentifier(cb, personIdentifier)
}

fun authorisationMatchesPersonName(name: String) = Specification<TemporaryAbsenceAuthorisation> { taa, _, cb ->
  taa.join<TemporaryAbsenceAuthorisation, PersonSummary>(PERSON, JoinType.INNER).matchesName(cb, name)
}

fun authorisationOverlapsDateRange(start: LocalDate, end: LocalDate) = Specification<TemporaryAbsenceAuthorisation> { taa, _, cb ->
  cb.and(
    cb.greaterThanOrEqualTo(taa.get(END), start),
    cb.lessThanOrEqualTo(taa.get(START), end),
  )
}

fun authorisationStatusCodeIn(statusCodes: Set<AuthorisationStatus.Code>) = Specification<TemporaryAbsenceAuthorisation> { taa, _, _ ->
  val status = taa.join<TemporaryAbsenceAuthorisation, AuthorisationStatus>(STATUS, JoinType.INNER)
  status.get<String>(CODE).`in`(statusCodes.map { it.name })
}

fun AbsenceCategorisationFilter.matchesAuthorisation() = Specification<TemporaryAbsenceAuthorisation> { taa, _, _ ->
  val fieldName = when (domainCode) {
    ReferenceDataDomain.Code.ABSENCE_TYPE -> TemporaryAbsenceAuthorisation.ABSENCE_TYPE
    ReferenceDataDomain.Code.ABSENCE_SUB_TYPE -> TemporaryAbsenceAuthorisation.ABSENCE_SUB_TYPE
    ReferenceDataDomain.Code.ABSENCE_REASON_CATEGORY -> TemporaryAbsenceAuthorisation.ABSENCE_REASON_CATEGORY
    ReferenceDataDomain.Code.ABSENCE_REASON -> TemporaryAbsenceAuthorisation.ABSENCE_REASON
    else -> throw IllegalArgumentException("Not a valid absence categorisation filter")
  }
  val rd = taa.join<TemporaryAbsenceAuthorisation, ReferenceData>(fieldName, JoinType.INNER)
  rd.get<String>(CODE).`in`(codes)
}
