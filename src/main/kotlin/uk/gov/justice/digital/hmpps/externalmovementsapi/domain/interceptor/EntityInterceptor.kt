package uk.gov.justice.digital.hmpps.externalmovementsapi.domain.interceptor

import org.hibernate.Interceptor
import org.hibernate.type.Type
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.TemporaryAbsenceAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.TemporaryAbsenceOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.TapAuthorisationStatus
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.HmppsDomainEvent
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.HmppsDomainEventRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.PersonReference
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.TemporaryAbsenceAuthorised
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.TemporaryAbsenceAuthorisedInformation
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.TemporaryAbsenceScheduled
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.TemporaryAbsenceScheduledInformation

@Component
class EntityInterceptor : Interceptor {
  private lateinit var domainEvents: HmppsDomainEventRepository

  @Autowired
  fun setDomainEventRepository(@Lazy domainEvents: HmppsDomainEventRepository) {
    this.domainEvents = domainEvents
  }

  override fun onFlushDirty(
    entity: Any,
    id: Any,
    currentState: Array<out Any>,
    previousState: Array<out Any>,
    propertyNames: Array<out String>,
    types: Array<out Type>,
  ): Boolean {
    if (entity is TemporaryAbsenceAuthorisation && entity.status.code == TapAuthorisationStatus.Code.APPROVED.name) {
      val index = propertyNames.indexOf(TemporaryAbsenceAuthorisation::status.name)
      val previousStatus = previousState[index]
      val previousStatusCode = if (previousStatus is TapAuthorisationStatus) {
        previousStatus.code
      } else {
        null
      }

      if (previousStatusCode != entity.status.code) {
        val event = TemporaryAbsenceAuthorised(
          TemporaryAbsenceAuthorisedInformation(entity.id),
          PersonReference.withIdentifier(entity.personIdentifier),
        )
        domainEvents.save(HmppsDomainEvent(event))
      }
    }
    return super.onFlushDirty(entity, id, currentState, previousState, propertyNames, types)
  }

  override fun onPersist(
    entity: Any,
    id: Any,
    state: Array<out Any>,
    propertyNames: Array<out String>,
    types: Array<out Type>,
  ): Boolean {
    if (entity is TemporaryAbsenceAuthorisation && entity.status.code == TapAuthorisationStatus.Code.APPROVED.name) {
      val event = TemporaryAbsenceAuthorised(
        TemporaryAbsenceAuthorisedInformation(entity.id),
        PersonReference.withIdentifier(entity.personIdentifier),
      )
      domainEvents.save(HmppsDomainEvent(event))
    }
    if (entity is TemporaryAbsenceOccurrence) {
      val event = TemporaryAbsenceScheduled(
        TemporaryAbsenceScheduledInformation(entity.id),
        PersonReference.withIdentifier(entity.personIdentifier),
      )
      domainEvents.save(HmppsDomainEvent(event))
    }
    return super.onPersist(entity, id, state, propertyNames, types)
  }
}
