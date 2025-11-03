package uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.occurrence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.envers.Audited
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.IdGenerator.newUuid
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.interceptor.Action
import java.util.UUID

@Audited
@Entity
@Table(name = "tap_occurrence_action")
class TapOccurrenceAction(
  @ManyToOne(fetch = FetchType.LAZY)
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
