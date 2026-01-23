package uk.gov.justice.digital.hmpps.externalmovementsapi.service.history

import jakarta.persistence.EntityManager
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.movement.TemporaryAbsenceMovement
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.manageusers.ManageUsersClient
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.AuditedAction

@Service
class MovementHistory(
  entityManager: EntityManager,
  managerUsers: ManageUsersClient,
) : HistoryService<TemporaryAbsenceMovement>(entityManager, managerUsers) {
  override val entityClass = TemporaryAbsenceMovement::class.java
  override fun TemporaryAbsenceMovement.changesFrom(previous: TemporaryAbsenceMovement): List<AuditedAction.Change> = TemporaryAbsenceMovement.changeableProperties().mapNotNull {
    val change = it.invoke(this).asChangeValue()
    val previous = it.invoke(previous).asChangeValue()
    if (change != previous) {
      AuditedAction.Change(it.name, previous, change)
    } else {
      null
    }
  }
}
