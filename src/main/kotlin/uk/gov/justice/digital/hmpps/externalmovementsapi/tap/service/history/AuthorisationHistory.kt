package uk.gov.justice.digital.hmpps.externalmovementsapi.tap.service.history

import jakarta.persistence.EntityManager
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.externalmovementsapi.audit.HistoryService
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.manageusers.ManageUsersClient
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.AuditedAction
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.domain.authorisation.TemporaryAbsenceAuthorisation

@Service
class AuthorisationHistory(
  entityManager: EntityManager,
  managerUsers: ManageUsersClient,
) : HistoryService<TemporaryAbsenceAuthorisation>(entityManager, managerUsers) {
  override val entityClass = TemporaryAbsenceAuthorisation::class.java
  override fun TemporaryAbsenceAuthorisation.changesFrom(previous: TemporaryAbsenceAuthorisation): List<AuditedAction.Change> = TemporaryAbsenceAuthorisation.changeableProperties().mapNotNull {
    val change = it(this).asChangeValue()
    val previous = it(previous).asChangeValue()
    if (change != previous) {
      AuditedAction.Change(it.name, previous, change)
    } else {
      null
    }
  }
}
