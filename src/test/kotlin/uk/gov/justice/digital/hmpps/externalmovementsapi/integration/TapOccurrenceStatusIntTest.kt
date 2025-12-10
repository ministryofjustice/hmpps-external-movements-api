package uk.gov.justice.digital.hmpps.externalmovementsapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.hibernate.envers.RevisionType
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.movement.TemporaryAbsenceMovement
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.occurrence.TemporaryAbsenceOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.TapOccurrenceStatus
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.HmppsDomainEvent
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.TemporaryAbsenceExpired
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.TemporaryAbsenceOverdue
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceAuthorisationOperations
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceAuthorisationOperations.Companion.temporaryAbsenceAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceMovementOperations
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceMovementOperations.Companion.temporaryAbsenceMovement
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceOccurrenceOperations
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceOccurrenceOperations.Companion.temporaryAbsenceOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.service.OccurrenceStatusUpdate
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

class TapOccurrenceStatusIntTest(
  @Autowired private val taaOperations: TempAbsenceAuthorisationOperations,
  @Autowired private val taoOperations: TempAbsenceOccurrenceOperations,
  @Autowired private val tamOperations: TempAbsenceMovementOperations,
  @Autowired private val update: OccurrenceStatusUpdate,
) : IntegrationTest(),
  TempAbsenceAuthorisationOperations by taaOperations,
  TempAbsenceOccurrenceOperations by taoOperations,
  TempAbsenceMovementOperations by tamOperations {

  @Test
  fun `occurrence status updated to overdue`() {
    val durationAhead = Duration.ofMillis(500)
    val auth = givenTemporaryAbsenceAuthorisation(temporaryAbsenceAuthorisation())
    val occurrence = givenTemporaryAbsenceOccurrence(
      temporaryAbsenceOccurrence(
        auth,
        start = LocalDateTime.now().minusHours(2),
        end = LocalDateTime.now().plus(durationAhead),
        movements = listOf(
          temporaryAbsenceMovement(TemporaryAbsenceMovement.Direction.OUT, auth.person.identifier),
        ),
      ),
    )
    assertThat(occurrence.status.code).isEqualTo(TapOccurrenceStatus.Code.IN_PROGRESS.name)

    TimeUnit.MILLISECONDS.sleep(durationAhead.toMillis() + 100)
    update.pastOccurrencesOfInterest()

    val updated = requireNotNull(findTemporaryAbsenceOccurrence(occurrence.id))
    assertThat(updated.status.code).isEqualTo(TapOccurrenceStatus.Code.OVERDUE.name)

    verifyAudit(updated, RevisionType.MOD, setOf(TemporaryAbsenceOccurrence::class.simpleName!!, HmppsDomainEvent::class.simpleName!!))
    verifyEvents(updated, setOf(TemporaryAbsenceOverdue(occurrence.authorisation.person.identifier, occurrence.id)))
  }

  @Test
  fun `occurrence status updated to expired when scheduled`() {
    val durationAhead = Duration.ofMillis(100)
    val auth = givenTemporaryAbsenceAuthorisation(temporaryAbsenceAuthorisation())
    val occurrence = givenTemporaryAbsenceOccurrence(
      temporaryAbsenceOccurrence(
        auth,
        start = LocalDateTime.now().minusHours(2),
        end = LocalDateTime.now().plus(durationAhead),
      ),
    )
    assertThat(occurrence.status.code).isEqualTo(TapOccurrenceStatus.Code.SCHEDULED.name)

    TimeUnit.MILLISECONDS.sleep(durationAhead.toMillis() + 100)
    update.pastOccurrencesOfInterest()

    val updated = requireNotNull(findTemporaryAbsenceOccurrence(occurrence.id))
    assertThat(updated.status.code).isEqualTo(TapOccurrenceStatus.Code.EXPIRED.name)

    verifyAudit(updated, RevisionType.MOD, setOf(TemporaryAbsenceOccurrence::class.simpleName!!, HmppsDomainEvent::class.simpleName!!))
    verifyEvents(updated, setOf(TemporaryAbsenceExpired(occurrence.authorisation.person.identifier, occurrence.id)))
  }
}
