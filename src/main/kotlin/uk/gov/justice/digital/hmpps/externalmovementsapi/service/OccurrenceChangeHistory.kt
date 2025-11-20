package uk.gov.justice.digital.hmpps.externalmovementsapi.service

import jakarta.persistence.EntityManager
import org.hibernate.envers.AuditReaderFactory
import org.hibernate.envers.RevisionType
import org.hibernate.envers.query.AuditEntity
import org.hibernate.envers.query.AuditEntity.revisionNumber
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.externalmovementsapi.audit.AuditRevision
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.occurrence.TemporaryAbsenceOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceData
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.HmppsDomainEvent
import uk.gov.justice.digital.hmpps.externalmovementsapi.exception.NotFoundException
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.manageusers.ManageUsersClient
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.manageusers.UserDetails
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.AuditHistory
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.AuditedAction
import java.util.UUID

@Service
@Transactional(readOnly = true)
class OccurrenceChangeHistory(
  private val entityManager: EntityManager,
  private val managerUsers: ManageUsersClient,
) {
  fun changes(id: UUID): AuditHistory {
    val audited = getAuditRecords(id)
    if (audited.isEmpty()) {
      throw NotFoundException("Temporary absence not found")
    }
    val domainEvents = getDomainEvents(audited.mapNotNull { it.revision.id }.toSet())
    val users =
      managerUsers.getUsersDetails(audited.mapNotNull { it.revision.username }.toSet()).associateBy { it.username }
    return AuditHistory(
      audited.actions({ domainEvents[it] ?: emptyList() }, { requireNotNull(users[it]) }),
    )
  }

  private fun getAuditRecords(id: UUID): List<AuditedOccurrence> {
    val auditReader = AuditReaderFactory.get(entityManager)
    val entityRevisions: List<Array<*>> =
      auditReader
        .createQuery()
        .forRevisionsOfEntity(TemporaryAbsenceOccurrence::class.java, false, false)
        .add(AuditEntity.id().eq(id))
        .resultList.filterIsInstance<Array<*>>()

    return entityRevisions.map { it.asAuditedAction() }.sortedBy { it.revision.timestamp }
  }

  private fun getDomainEvents(revisionIds: Set<Long>): Map<Long, List<String>> {
    val auditReader = AuditReaderFactory.get(entityManager)
    return auditReader
      .createQuery()
      .forRevisionsOfEntity(HmppsDomainEvent::class.java, false, false)
      .add(revisionNumber().`in`(revisionIds))
      .addProjection(revisionNumber())
      .addProjection(AuditEntity.property("eventType"))
      .resultList.filterIsInstance<Array<*>>()
      .map { it[0] as Long to it[1] as String }.groupBy({ it.first }, { it.second })
  }

  private fun Array<*>.asAuditedAction(): AuditedOccurrence {
    val entity = this[0] as TemporaryAbsenceOccurrence
    val revision = this[1] as AuditRevision
    val type = this[2] as RevisionType
    return AuditedOccurrence(type, revision, entity)
  }

  private fun List<AuditedOccurrence>.actions(
    events: (Long) -> List<String>,
    user: (String) -> UserDetails,
  ): List<AuditedAction> = mapIndexed { idx, audited ->
    val user = user(audited.revision.username!!)
    val de = events(audited.revision.id!!)
    if (idx == 0) {
      AuditedAction(
        AuditedAction.User(user.username, user.name),
        audited.revision.timestamp!!,
        de,
        audited.revision.reason,
        listOf(),
      )
    } else {
      AuditedAction(
        AuditedAction.User(user.username, user.name),
        audited.revision.timestamp!!,
        de,
        audited.revision.reason,
        audited.occurrence.changesFrom(this[idx - 1].occurrence),
      )
    }
  }

  fun TemporaryAbsenceOccurrence.changesFrom(previous: TemporaryAbsenceOccurrence): List<AuditedAction.Change> = TemporaryAbsenceOccurrence.changeableProperties().mapNotNull {
    val change = it.invoke(this).asChangeValue()
    val previous = it.invoke(previous).asChangeValue()
    if (change != previous) {
      AuditedAction.Change(it.name, previous, change)
    } else {
      null
    }
  }

  private fun Any?.asChangeValue(): Any? = when (this) {
    is ReferenceData -> this.description
    else -> this
  }

  class AuditedOccurrence(
    val type: RevisionType,
    val revision: AuditRevision,
    val occurrence: TemporaryAbsenceOccurrence,
  )
}
