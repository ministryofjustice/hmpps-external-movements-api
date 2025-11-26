package uk.gov.justice.digital.hmpps.externalmovementsapi.service.history

import jakarta.persistence.EntityManager
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.authorisation.TemporaryAbsenceAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.manageusers.ManageUsersClient
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.AuditedAction

@Service
class AuthorisationChangeHistory(
  entityManager: EntityManager,
  managerUsers: ManageUsersClient,
) : ChangeHistoryService<TemporaryAbsenceAuthorisation>(entityManager, managerUsers) {
  override val entityClass = TemporaryAbsenceAuthorisation::class.java
  override fun TemporaryAbsenceAuthorisation.changesFrom(previous: TemporaryAbsenceAuthorisation): List<AuditedAction.Change> = TemporaryAbsenceAuthorisation.changeableProperties().mapNotNull {
    val change = it.invoke(this).asChangeValue()
    val previous = it.invoke(previous).asChangeValue()
    if (change != previous) {
      AuditedAction.Change(it.name, previous, change)
    } else {
      null
    }
  }
}
