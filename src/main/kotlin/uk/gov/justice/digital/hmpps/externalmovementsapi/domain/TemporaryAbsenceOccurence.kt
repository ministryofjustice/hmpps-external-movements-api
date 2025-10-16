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
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.TemporaryAbsenceAuthorisation.Companion.PRISON_CODE
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.TemporaryAbsenceOccurrence.Companion.AUTHORISATION
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.TemporaryAbsenceOccurrence.Companion.PERSON_IDENTIFIER
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.TemporaryAbsenceOccurrence.Companion.RELEASE_AT
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.TemporaryAbsenceOccurrence.Companion.RETURN_BY
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.AccompaniedBy
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceData
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.Transport
import uk.gov.justice.digital.hmpps.externalmovementsapi.exception.NotFoundException
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.location.Location
import uk.gov.justice.digital.hmpps.externalmovementsapi.sync.ScheduledTemporaryAbsenceRequest
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Audited
@Entity
@Table(name = "temporary_absence_occurrence")
class TemporaryAbsenceOccurrence(
  @Audited
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "authorisation_id")
  val authorisation: TemporaryAbsenceAuthorisation,
  releaseAt: LocalDateTime,
  returnBy: LocalDateTime,
  accompaniedBy: AccompaniedBy,
  transport: Transport,
  location: Location,
  contactInformation: String?,
  notes: String?,
  addedAt: LocalDateTime,
  addedBy: String,
  cancelledAt: LocalDateTime?,
  cancelledBy: String?,
  scheduleReference: JsonNode?,
  legacyId: Long?,
  @Id
  @Column(name = "id", nullable = false)
  val id: UUID = newUuid(),
) {
  @Size(max = 10)
  @NotNull
  @Column(name = "person_identifier", nullable = false, length = 10)
  var personIdentifier: String = authorisation.personIdentifier
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
  @JoinColumn(name = "accompanied_by_id")
  var accompaniedBy: AccompaniedBy = accompaniedBy
    private set

  @Audited(targetAuditMode = NOT_AUDITED)
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "transport_id")
  var transport: Transport = transport
    private set

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "location")
  var location: Location = location
    private set

  @Column(name = "contact_information")
  var contactInformation: String? = contactInformation
    private set

  @Column(name = "notes")
  var notes: String? = notes
    private set

  @NotNull
  @Column(name = "added_at", nullable = false)
  var addedAt: LocalDateTime = addedAt
    private set

  @NotNull
  @Column(name = "added_by", nullable = false)
  var addedBy: String = addedBy
    private set

  @Column(name = "cancelled_at")
  var cancelledAt: LocalDateTime? = cancelledAt
    private set

  @Column(name = "cancelled_by", nullable = false)
  var cancelledBy: String? = cancelledBy
    private set

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "schedule_reference")
  var scheduleReference: JsonNode? = scheduleReference
    private set

  @Column(name = "legacy_id")
  var legacyId: Long? = legacyId
    private set

  @Version
  var version: Int? = null
    private set

  fun update(
    request: ScheduledTemporaryAbsenceRequest,
    rdProvider: (ReferenceDataDomain.Code, String) -> ReferenceData,
  ) = apply {
    releaseAt = request.startTime
    returnBy = request.returnTime
    contactInformation = request.contactPersonName
    location = request.location.asLocation()
    accompaniedBy = rdProvider(ReferenceDataDomain.Code.ACCOMPANIED_BY, request.escortOrDefault()) as AccompaniedBy
    transport = rdProvider(ReferenceDataDomain.Code.TRANSPORT, request.transportTypeOrDefault()) as Transport
    notes = request.comment
    addedAt = request.audit.createDatetime
    addedBy = request.audit.createUsername
    cancelledAt = request.cancelledAt
    cancelledBy = request.cancelledBy
    legacyId = request.eventId
  }

  companion object {
    val AUTHORISATION = TemporaryAbsenceOccurrence::authorisation.name
    val PERSON_IDENTIFIER = TemporaryAbsenceOccurrence::personIdentifier.name
    val RELEASE_AT = TemporaryAbsenceOccurrence::releaseAt.name
    val RETURN_BY = TemporaryAbsenceOccurrence::returnBy.name
  }
}

interface TemporaryAbsenceOccurrenceRepository :
  JpaRepository<TemporaryAbsenceOccurrence, UUID>,
  JpaSpecificationExecutor<TemporaryAbsenceOccurrence> {
  fun findByPersonIdentifierAndReleaseAtAndReturnBy(
    personIdentifier: String,
    releaseAt: LocalDateTime,
    returnBy: LocalDateTime,
  ): TemporaryAbsenceOccurrence?

  fun findByLegacyId(legacyId: Long): TemporaryAbsenceOccurrence?

  fun findByAuthorisationId(authorisationId: UUID): List<TemporaryAbsenceOccurrence>
  fun findByAuthorisationIdIn(authorisationIds: Set<UUID>): List<TemporaryAbsenceOccurrence>

  @Query(
    """
    select
    sum(case when tao.release_at between current_date and (current_date + 1) then 1 else 0 end) as leavingToday,
    sum(case when tao.release_at between (current_date + 1) and (current_date + 8) then 1 else 0 end) as leavingNextSevenDays
    from temporary_absence_occurrence tao
        join temporary_absence_authorisation taa on taa.id = tao.authorisation_id
        join reference_data st on st.id = taa.status_id
    where taa.prison_code = :prisonIdentifier
      and st.code = 'APPROVED'
      and tao.release_at between current_date and (current_date + 8)
    group by taa.prison_code
  """,
    nativeQuery = true,
  )
  fun findUpcomingLeaverCounts(prisonIdentifier: String): PrisonLeaverCounts?

  @Query(
    """
    select count(1) as returningToday
    from temporary_absence_occurrence tao
        join temporary_absence_authorisation taa on taa.id = tao.authorisation_id
        join reference_data st on st.id = taa.status_id
    where taa.prison_code = :prisonIdentifier
      and st.code = 'APPROVED'
      and tao.return_by between current_date and (current_date + 1)
  """,
    nativeQuery = true,
  )
  fun findReturningTodayCount(prisonIdentifier: String): Int
}

interface PrisonLeaverCounts {
  val leavingToday: Int
  val leavingNextSevenDays: Int

  data object Default : PrisonLeaverCounts {
    override val leavingToday: Int = 0
    override val leavingNextSevenDays: Int = 0
  }
}

fun TemporaryAbsenceOccurrenceRepository.getOccurrence(id: UUID) = findByIdOrNull(id) ?: throw NotFoundException("Temporary absence occurrence not found")

fun occurrenceMatchesPrisonCode(prisonCode: String) = Specification<TemporaryAbsenceOccurrence> { tao, _, cb ->
  val authorisation = tao.join<TemporaryAbsenceOccurrence, TemporaryAbsenceAuthorisation>(AUTHORISATION, JoinType.INNER)
  cb.equal(authorisation.get<String>(PRISON_CODE), prisonCode)
}

fun occurrenceMatchesPersonIdentifier(personIdentifier: String) = Specification<TemporaryAbsenceOccurrence> { tao, _, cb ->
  cb.equal(tao.get<String>(PERSON_IDENTIFIER), personIdentifier)
}

fun occurrenceMatchesDateRange(fromDate: LocalDate, toDate: LocalDate) = Specification<TemporaryAbsenceOccurrence> { tao, _, cb ->
  cb.and(
    cb.greaterThanOrEqualTo(tao.get(RELEASE_AT), fromDate.atStartOfDay()),
    cb.lessThanOrEqualTo(tao.get(RETURN_BY), toDate.plusDays(1).atStartOfDay()),
  )
}
