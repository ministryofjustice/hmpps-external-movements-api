package uk.gov.justice.digital.hmpps.externalmovementsapi.tap.service.history

import jakarta.persistence.EntityManager
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.externalmovementsapi.audit.HistoryService
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.manageusers.ManageUsersClient
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.AuditedAction
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.domain.movement.TemporaryAbsenceMovement

@Service
class MovementHistory(
  entityManager: EntityManager,
  managerUsers: ManageUsersClient,
) : HistoryService<TemporaryAbsenceMovement>(entityManager, managerUsers) {
  override val entityClass = TemporaryAbsenceMovement::class.java
  override fun TemporaryAbsenceMovement.changesFrom(previous: TemporaryAbsenceMovement): List<AuditedAction.Change> = TemporaryAbsenceMovement.changeableProperties().mapNotNull {
    val change = it(this).asChangeValue()
    val previous = it(previous).asChangeValue()
    if (change != previous) {
      AuditedAction.Change(it.name, previous, change)
    } else {
      null
    }
  }
}
