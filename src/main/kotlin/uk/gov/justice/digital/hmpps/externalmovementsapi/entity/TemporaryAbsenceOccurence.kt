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
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.AccompaniedBy
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.LocationType
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.TapOccurrenceStatus
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.Transport
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
  accompaniedBy: AccompaniedBy?,
  transport: Transport?,
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
  var accompaniedBy: AccompaniedBy? = accompaniedBy
    private set

  @Audited(targetAuditMode = NOT_AUDITED)
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "transport_id")
  var transport: Transport? = transport
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
}

interface TemporaryAbsenceOccurrenceRepository : JpaRepository<TemporaryAbsenceOccurrence, UUID> {
  fun findByPersonIdentifierAndReleaseAtAndReturnBy(
    personIdentifier: String,
    releaseAt: LocalDateTime,
    returnBy: LocalDateTime,
  ): TemporaryAbsenceOccurrence?

  fun findByAuthorisationId(authorisationId: UUID): List<TemporaryAbsenceOccurrence>
}
