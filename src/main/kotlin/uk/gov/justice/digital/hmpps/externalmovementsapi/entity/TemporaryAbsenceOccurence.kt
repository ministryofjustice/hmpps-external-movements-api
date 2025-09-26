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
import org.springframework.data.jpa.repository.Query
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.IdGenerator.newUuid
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.AccompaniedBy
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.LocationType
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.ReferenceData
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.ReferenceDataDomain
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.TapOccurrenceStatus
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.Transport
import uk.gov.justice.digital.hmpps.externalmovementsapi.sync.ScheduledTemporaryAbsenceRequest
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
  locationType: LocationType,
  locationId: String?,
  accompaniedBy: AccompaniedBy,
  transport: Transport,
  contact: String?,
  notes: String?,
  status: TapOccurrenceStatus,
  addedAt: LocalDateTime,
  addedBy: String,
  cancelledAt: LocalDateTime?,
  cancelledBy: String?,
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
  @JoinColumn(name = "location_type_id")
  var locationType: LocationType = locationType
    private set

  @Size(max = 36)
  @Column(name = "location_id", length = 36)
  var locationId: String? = locationId
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

  @Column(name = "contact")
  var contact: String? = contact
    private set

  @Column(name = "notes")
  var notes: String? = notes
    private set

  @Audited(targetAuditMode = NOT_AUDITED)
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "status_id", nullable = false)
  var status: TapOccurrenceStatus = status
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
    status = rdProvider(
      ReferenceDataDomain.Code.TAP_OCCURRENCE_STATUS,
      request.occurrenceStatusCode.name,
    ) as TapOccurrenceStatus
    locationType =
      request.toAddressOwnerClass?.let { rdProvider(ReferenceDataDomain.Code.LOCATION_TYPE, it) as? LocationType }
        ?: rdProvider(ReferenceDataDomain.Code.LOCATION_TYPE, "OTHER") as LocationType
    locationId = request.toAddressId?.toString()
    contact = request.contactPersonName
    accompaniedBy = rdProvider(ReferenceDataDomain.Code.ACCOMPANIED_BY, request.escortOrDefault()) as AccompaniedBy
    transport = rdProvider(ReferenceDataDomain.Code.TRANSPORT, request.transportTypeOrDefault()) as Transport
    notes = request.comment
    addedAt = request.audit.createDatetime
    addedBy = request.audit.createUsername
    cancelledAt = request.cancelledAt
    cancelledBy = request.cancelledBy
    legacyId = request.eventId
  }
}

interface TemporaryAbsenceOccurrenceRepository : JpaRepository<TemporaryAbsenceOccurrence, UUID> {
  fun findByPersonIdentifierAndReleaseAtAndReturnBy(
    personIdentifier: String,
    releaseAt: LocalDateTime,
    returnBy: LocalDateTime,
  ): TemporaryAbsenceOccurrence?

  fun findByLegacyId(legacyId: Long): TemporaryAbsenceOccurrence?

  fun findByAuthorisationId(authorisationId: UUID): List<TemporaryAbsenceOccurrence>

  @Query(
    """
    select
    sum(case when tao.release_at between current_date and (current_date + 1) then 1 else 0 end) as leavingToday,
    sum(case when tao.release_at between (current_date + 1) and (current_date + 8) then 1 else 0 end) as leavingNextSevenDays
    from temporary_absence_occurrence tao
        join temporary_absence_authorisation taa on taa.id = tao.authorisation_id
        join reference_data st on st.id = tao.status_id
    where taa.prison_code = :prisonIdentifier
      and st.code = 'SCHEDULED'
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
        join reference_data st on st.id = tao.status_id
    where taa.prison_code = :prisonIdentifier
      and st.code = 'SCHEDULED'
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
