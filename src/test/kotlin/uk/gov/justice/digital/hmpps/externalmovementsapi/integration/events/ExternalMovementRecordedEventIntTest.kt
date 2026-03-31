package uk.gov.justice.digital.hmpps.externalmovementsapi.integration.events

import org.assertj.core.api.Assertions.assertThat
import org.hibernate.envers.RevisionType
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNull
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.ExternalMovementContext
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.event.producer.publication
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.authorisation.TemporaryAbsenceAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.movement.TemporaryAbsenceMovement.Direction
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.occurrence.TemporaryAbsenceOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.AuthorisationStatus
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.OccurrenceStatus
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.HmppsDomainEvent
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.TemporaryAbsenceAuthorisationApproved
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.TemporaryAbsenceAuthorisationCancelled
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.TemporaryAbsenceCancelled
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.TemporaryAbsenceUnScheduled
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.DataGenerator.prisonCode
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.IntegrationTest
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.LocationGenerator.location
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceAuthorisationOperations
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceAuthorisationOperations.Companion.temporaryAbsenceAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceMovementOperations.Companion.temporaryAbsenceMovement
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceOccurrenceOperations
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceOccurrenceOperations.Companion.temporaryAbsenceOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.wiremock.PrisonerRegisterExtension.Companion.prisonRegister
import uk.gov.justice.digital.hmpps.externalmovementsapi.service.ExternalMovementRecordedEvent
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit.SECONDS

class ExternalMovementRecordedEventIntTest(
  @Autowired private val taa: TempAbsenceAuthorisationOperations,
  @Autowired private val tao: TempAbsenceOccurrenceOperations,
) : IntegrationTest(),
  TempAbsenceAuthorisationOperations by taa,
  TempAbsenceOccurrenceOperations by tao {

  @Test
  fun `movement recorded for transfer out to another prison`() {
    val fromPrison = givenPrison()
    val toPrison = givenPrison()
    prisonRegister.getPrisons(setOf(toPrison))
    val auth = givenTemporaryAbsenceAuthorisation(
      temporaryAbsenceAuthorisation(
        prisonCode = fromPrison.code,
        repeat = true,
        locations = linkedSetOf(location()),
      ),
    )
    val occurrences = (0L..2L).map {
      givenTemporaryAbsenceOccurrence(
        temporaryAbsenceOccurrence(
          auth,
          start = LocalDateTime.now().plusDays(it).plusHours(1).truncatedTo(SECONDS),
          end = LocalDateTime.now().plusDays(it).plusHours(3).truncatedTo(SECONDS),
          location = auth.locations.first,
          movements = if (it == 0L) {
            listOf(
              temporaryAbsenceMovement(
                Direction.OUT,
                auth.person.identifier,
                occurredAt = LocalDateTime.now(),
                prisonCode = auth.prisonCode,
                location = auth.locations.first,
              ),
            )
          } else {
            listOf()
          },
        ),
      )
    }

    val event = emRecordedEvent(auth.person.identifier, "TRN", toAgencyId = toPrison.code)
    sendOffenderEvent(event)

    waitUntil { requireNotNull(findTemporaryAbsenceAuthorisation(auth.id)?.version) > 0 }

    val savedAuth = requireNotNull(findTemporaryAbsenceAuthorisation(auth.id))
    assertThat(savedAuth.status.code).isEqualTo(AuthorisationStatus.Code.CANCELLED.name)

    val firstOccurrence = requireNotNull(findTemporaryAbsenceOccurrence(occurrences.first().id))
    assertThat(firstOccurrence.status.code).isEqualTo(OccurrenceStatus.Code.IN_PROGRESS.name)

    occurrences.takeLast(2).forEach { assertNull(findTemporaryAbsenceOccurrence(it.id)) }

    verifyAudit(
      savedAuth,
      RevisionType.MOD,
      setOf(
        HmppsDomainEvent::class.simpleName!!,
        TemporaryAbsenceAuthorisation::class.simpleName!!,
        TemporaryAbsenceOccurrence::class.simpleName!!,
      ),
      ExternalMovementContext.get().copy(reason = "Transferred"),
    )

    listOf(occurrences[1], occurrences[2]).forEach {
      verifyAudit(
        it,
        RevisionType.DEL,
        setOf(
          HmppsDomainEvent::class.simpleName!!,
          TemporaryAbsenceAuthorisation::class.simpleName!!,
          TemporaryAbsenceOccurrence::class.simpleName!!,
        ),
        ExternalMovementContext.get().copy(reason = "Transferred"),
      )
    }

    verifyEventPublications(
      savedAuth,
      setOf(
        TemporaryAbsenceAuthorisationCancelled(savedAuth.person.identifier, savedAuth.id).publication(savedAuth.id),
        TemporaryAbsenceUnScheduled(savedAuth.person.identifier, occurrences[1].id).publication(occurrences[1].id),
        TemporaryAbsenceUnScheduled(savedAuth.person.identifier, occurrences[2].id).publication(occurrences[2].id),
      ),
    )
  }

  @Test
  fun `movement recorded for released`() {
    val fromPrison = givenPrison()
    prisonRegister.getPrisons(setOf(), setOf("OUT"))
    val auth = givenTemporaryAbsenceAuthorisation(
      temporaryAbsenceAuthorisation(
        prisonCode = fromPrison.code,
        repeat = false,
        locations = linkedSetOf(location()),
      ),
    )
    val occurrence = givenTemporaryAbsenceOccurrence(
      temporaryAbsenceOccurrence(
        auth,
        start = LocalDateTime.now().plusDays(1).plusHours(1).truncatedTo(SECONDS),
        end = LocalDateTime.now().plusDays(2).plusHours(3).truncatedTo(SECONDS),
        location = auth.locations.first,
        movements = listOf(),
      ),
    )
    assertThat(occurrence.status.code).isEqualTo(OccurrenceStatus.Code.SCHEDULED.name)

    val event = emRecordedEvent(auth.person.identifier, "REL", toAgencyId = "OUT")
    sendOffenderEvent(event)
    waitUntil { requireNotNull(findTemporaryAbsenceAuthorisation(auth.id)?.version) > 0 }

    val savedAuth = requireNotNull(findTemporaryAbsenceAuthorisation(auth.id))
    assertThat(savedAuth.status.code).isEqualTo(AuthorisationStatus.Code.CANCELLED.name)

    val savedOccurrence = requireNotNull(findTemporaryAbsenceOccurrence(occurrence.id))
    assertThat(savedOccurrence.status.code).isEqualTo(OccurrenceStatus.Code.CANCELLED.name)
    assertThat(savedOccurrence.dpsOnly).isTrue

    listOf(savedAuth, savedOccurrence).forEach {
      verifyAudit(
        it,
        RevisionType.MOD,
        setOf(
          HmppsDomainEvent::class.simpleName!!,
          TemporaryAbsenceAuthorisation::class.simpleName!!,
          TemporaryAbsenceOccurrence::class.simpleName!!,
        ),
        ExternalMovementContext.get().copy(reason = "Released"),
      )
    }

    verifyEventPublications(
      savedAuth,
      setOf(
        TemporaryAbsenceAuthorisationCancelled(savedAuth.person.identifier, savedAuth.id).publication(savedAuth.id),
        TemporaryAbsenceCancelled(savedAuth.person.identifier, occurrence.id).publication(occurrence.id) { false },
      ),
    )
  }

  @Test
  fun `movement recorded for transfer out to non prison`() {
    val fromPrison = givenPrison()
    val toAgencyId = prisonCode()
    prisonRegister.getPrisons(setOf(), setOf(toAgencyId))
    val auth = givenTemporaryAbsenceAuthorisation(
      temporaryAbsenceAuthorisation(
        prisonCode = fromPrison.code,
        repeat = true,
        locations = linkedSetOf(location()),
      ),
    )
    val occurrences = (0L..1L).map {
      givenTemporaryAbsenceOccurrence(
        temporaryAbsenceOccurrence(
          auth,
          start = LocalDateTime.now().plusDays(it).plusHours(1).truncatedTo(SECONDS),
          end = LocalDateTime.now().plusDays(it).plusHours(3).truncatedTo(SECONDS),
          location = auth.locations.first,
          movements = listOf(),
        ),
      )
    }

    val event = emRecordedEvent(auth.person.identifier, "TRN", "IN", toAgencyId)
    sendOffenderEvent(event)

    waitUntil { offenderEventsQueue.isEmpty() }

    val savedAuth = requireNotNull(findTemporaryAbsenceAuthorisation(auth.id))
    assertThat(savedAuth.status.code).isEqualTo(AuthorisationStatus.Code.APPROVED.name)

    occurrences.takeLast(2).forEach {
      val occ = requireNotNull(findTemporaryAbsenceOccurrence(it.id))
      assertThat(occ.status.code).isEqualTo(OccurrenceStatus.Code.SCHEDULED.name)
    }

    verifyAudit(
      savedAuth,
      RevisionType.ADD,
      setOf(
        HmppsDomainEvent::class.simpleName!!,
        TemporaryAbsenceAuthorisation::class.simpleName!!,
      ),
      ExternalMovementContext.get().copy(reason = null),
    )

    verifyEvents(savedAuth, setOf(TemporaryAbsenceAuthorisationApproved(savedAuth.person.identifier, savedAuth.id)))

    occurrences.forEach {
      verifyAudit(
        it,
        RevisionType.ADD,
        setOf(
          HmppsDomainEvent::class.simpleName!!,
          TemporaryAbsenceOccurrence::class.simpleName!!,
        ),
        ExternalMovementContext.get().copy(reason = null),
      )
    }
  }

  private fun emRecordedEvent(
    personIdentifier: String,
    movementType: String,
    directionCode: String = "OUT",
    toAgencyId: String = prisonCode(),
    movementDateTime: LocalDateTime = LocalDateTime.now(),
  ) = ExternalMovementRecordedEvent(personIdentifier, movementType, directionCode, toAgencyId, movementDateTime)
}
