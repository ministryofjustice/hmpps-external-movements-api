package uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.occurrence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.envers.Audited
import org.springframework.data.jpa.repository.JpaRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.IdGenerator.newUuid
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.interceptor.Action
import java.util.UUID

@Audited
@Entity
@Table(name = "tap_occurrence_action")
class TapOccurrenceAction(
  @ManyToOne
  @JoinColumn(name = "occurrence_id", updatable = false)
  val occurrence: TemporaryAbsenceOccurrence,
  @Column(name = "type", nullable = false)
  override val type: String,
  @Column(name = "reason")
  override val reason: String?,
  @Id
  @Column(name = "id", nullable = false)
  override val id: UUID = newUuid(),
) : Action

interface TapOccurrenceActionRepository : JpaRepository<TapOccurrenceAction, UUID> {
  fun findByOccurrenceId(occurrenceId: UUID): List<TapOccurrenceAction>
  fun findByOccurrenceIdIn(occurrenceIds: Set<UUID>): List<TapOccurrenceAction>
}
