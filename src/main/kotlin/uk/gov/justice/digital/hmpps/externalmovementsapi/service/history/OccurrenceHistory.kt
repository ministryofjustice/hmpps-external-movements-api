package uk.gov.justice.digital.hmpps.externalmovementsapi.service.history

import jakarta.persistence.EntityManager
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.occurrence.TemporaryAbsenceOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.manageusers.ManageUsersClient
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.AuditedAction

@Service
class OccurrenceHistory(
  entityManager: EntityManager,
  managerUsers: ManageUsersClient,
) : HistoryService<TemporaryAbsenceOccurrence>(entityManager, managerUsers) {
  override val entityClass = TemporaryAbsenceOccurrence::class.java
  override fun TemporaryAbsenceOccurrence.changesFrom(previous: TemporaryAbsenceOccurrence): List<AuditedAction.Change> = TemporaryAbsenceOccurrence.changeableProperties().mapNotNull {
    val change = it.invoke(this).asChangeValue()
    val previous = it.invoke(previous).asChangeValue()
    if (change != previous) {
      AuditedAction.Change(it.name, previous, change)
    } else {
      null
    }
  }
}
