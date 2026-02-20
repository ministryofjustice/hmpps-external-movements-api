package uk.gov.justice.digital.hmpps.externalmovementsapi.integration.events

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.DataSource
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.AuthorisationStatus
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.OccurrenceStatus
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.TemporaryAbsenceAuthorisationCancelled
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.TemporaryAbsenceAuthorisationDeferred
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.TemporaryAbsenceAuthorisationDenied
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.TemporaryAbsenceAuthorisationExpired
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.TemporaryAbsenceAuthorisationPending
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.DataGenerator.newId
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.IntegrationTest
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceAuthorisationOperations
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceAuthorisationOperations.Companion.temporaryAbsenceAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceOccurrenceOperations
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceOccurrenceOperations.Companion.temporaryAbsenceOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.authorisation.CancelAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.authorisation.DeferAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.authorisation.ExpireAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.location.Location
import uk.gov.justice.digital.hmpps.externalmovementsapi.sync.write.AuthorisationSchedule
import uk.gov.justice.digital.hmpps.externalmovementsapi.sync.write.UnapprovedAuthStatusHandler
import java.time.LocalDate
import java.time.LocalTime

class UnapprovedAuthorisationIntTest(
  @Autowired private val uasHandler: UnapprovedAuthStatusHandler,
  @Autowired private val taaOperations: TempAbsenceAuthorisationOperations,
  @Autowired private val taoOperations: TempAbsenceOccurrenceOperations,
) : IntegrationTest(),
  TempAbsenceAuthorisationOperations by taaOperations,
  TempAbsenceOccurrenceOperations by taoOperations {

  @Test
  fun `authorisation pending event creates pending occurrence if source is NOMIS`() {
    val auth = givenTemporaryAbsenceAuthorisation(
      temporaryAbsenceAuthorisation(
        status = AuthorisationStatus.Code.PENDING,
        repeat = false,
        start = LocalDate.now().plusDays(1),
        end = LocalDate.now().plusDays(1),
        schedule = objectMapper.valueToTree(AuthorisationSchedule(LocalTime.of(10, 0), LocalTime.of(12, 0))),
      ),
    )

    uasHandler.handle(TemporaryAbsenceAuthorisationPending(auth.person.identifier, auth.id, DataSource.NOMIS))

    val occurrence = findForAuthorisation(auth.id).single()
    assertThat(occurrence.status.code).isEqualTo(OccurrenceStatus.Code.PENDING.name)
    assertThat(occurrence.start.toLocalTime()).isEqualTo(LocalTime.of(10, 0))
    assertThat(occurrence.end.toLocalTime()).isEqualTo(LocalTime.of(12, 0))
    assertThat(occurrence.location).isEqualTo(Location.empty())
  }

  @Test
  fun `authorisation deferred event replaces existing occurrence if source is NOMIS`() {
    /*
    The authorisation update for a single will set the schedule on the authorisation.
    The occurrence being replaced would be deleted by a nomis event.
    The deletion would be by id and will be successful even if the occurrence has already been deleted.
    If it happens before this event is processed there will be nothing to delete but should be created as in the first test.
     */

    val auth = givenTemporaryAbsenceAuthorisation(
      temporaryAbsenceAuthorisation(
        status = AuthorisationStatus.Code.APPROVED,
        repeat = false,
        legacyId = newId(),
        start = LocalDate.now().plusDays(1),
        end = LocalDate.now().plusDays(1),
        schedule = objectMapper.valueToTree(AuthorisationSchedule(LocalTime.of(11, 15), LocalTime.of(13, 15))),
      ),
    )
    val occ = givenTemporaryAbsenceOccurrence(temporaryAbsenceOccurrence(auth, legacyId = newId()))

    val deferred = transactionTemplate.execute {
      val toDefer = requireNotNull(findTemporaryAbsenceAuthorisation(auth.id))
      toDefer.defer(DeferAuthorisation()) { domain, code ->
        requireNotNull(referenceDataRepository.findAll().first { domain.isInstance(it) && it.code == code })
      }
      toDefer
    }!!
    assertThat(deferred.status.code).isEqualTo(AuthorisationStatus.Code.PENDING.name)

    uasHandler.handle(TemporaryAbsenceAuthorisationDeferred(auth.person.identifier, auth.id, DataSource.NOMIS))

    assertThat(findTemporaryAbsenceOccurrence(occ.id)).isNull()

    val occurrence = findForAuthorisation(auth.id).single()
    assertThat(occurrence.status.code).isEqualTo(OccurrenceStatus.Code.PENDING.name)
    assertThat(occurrence.start.toLocalTime()).isEqualTo(LocalTime.of(11, 15))
    assertThat(occurrence.end.toLocalTime()).isEqualTo(LocalTime.of(13, 15))
    assertThat(occurrence.location).isEqualTo(occ.location)
  }

  @Test
  fun `authorisation cancelled event replaces existing occurrence if source is NOMIS`() {
    /*
    The authorisation update for a single will set the schedule on the authorisation.
    The occurrence being replaced would be deleted by a nomis event.
    The deletion would be by id and will be successful even if the occurrence has already been deleted.
    If it happens before this event is processed there will be nothing to delete but should be created as in the first test.
     */

    val auth = givenTemporaryAbsenceAuthorisation(
      temporaryAbsenceAuthorisation(
        status = AuthorisationStatus.Code.APPROVED,
        repeat = false,
        legacyId = newId(),
        start = LocalDate.now().plusDays(1),
        end = LocalDate.now().plusDays(1),
        schedule = objectMapper.valueToTree(AuthorisationSchedule(LocalTime.of(14, 30), LocalTime.of(18, 30))),
      ),
    )
    val occ = givenTemporaryAbsenceOccurrence(temporaryAbsenceOccurrence(auth, legacyId = newId()))

    val cancelled = transactionTemplate.execute {
      val toCancel = requireNotNull(findTemporaryAbsenceAuthorisation(auth.id))
      toCancel.cancel(CancelAuthorisation()) { domain, code ->
        requireNotNull(referenceDataRepository.findAll().first { domain.isInstance(it) && it.code == code })
      }
      toCancel
    }!!
    assertThat(cancelled.status.code).isEqualTo(AuthorisationStatus.Code.CANCELLED.name)

    uasHandler.handle(TemporaryAbsenceAuthorisationCancelled(auth.person.identifier, auth.id, DataSource.NOMIS))

    assertThat(findTemporaryAbsenceOccurrence(occ.id)).isNull()

    val occurrence = findForAuthorisation(auth.id).single()
    assertThat(occurrence.status.code).isEqualTo(OccurrenceStatus.Code.CANCELLED.name)
    assertThat(occurrence.start.toLocalTime()).isEqualTo(LocalTime.of(14, 30))
    assertThat(occurrence.end.toLocalTime()).isEqualTo(LocalTime.of(18, 30))
    assertThat(occurrence.location).isEqualTo(occ.location)
  }

  @Test
  fun `authorisation denied event for authorisation with already denied occurrence does not change occurrence`() {
    val auth = givenTemporaryAbsenceAuthorisation(
      temporaryAbsenceAuthorisation(
        status = AuthorisationStatus.Code.DENIED,
        repeat = false,
        legacyId = newId(),
        start = LocalDate.now().plusDays(1),
        end = LocalDate.now().plusDays(1),
        schedule = objectMapper.valueToTree(AuthorisationSchedule(LocalTime.of(8, 40), LocalTime.of(10, 40))),
      ),
    )
    val occ = givenTemporaryAbsenceOccurrence(
      temporaryAbsenceOccurrence(
        auth,
        start = auth.start.atTime(8, 40),
        end = auth.end.atTime(10, 40),
      ),
    )

    uasHandler.handle(TemporaryAbsenceAuthorisationDenied(auth.person.identifier, auth.id, DataSource.NOMIS))

    val occurrence = findForAuthorisation(auth.id).single()
    assertThat(occurrence.id).isEqualTo(occ.id)
    assertThat(occurrence.status.code).isEqualTo(OccurrenceStatus.Code.DENIED.name)
    assertThat(occurrence.start.toLocalTime()).isEqualTo(LocalTime.of(8, 40))
    assertThat(occurrence.end.toLocalTime()).isEqualTo(LocalTime.of(10, 40))
    assertThat(occurrence.location).isEqualTo(occ.location)
  }

  @Test
  fun `sync of an expired authorisation creates occurrence if one doesn't exist`() {
    val auth = givenTemporaryAbsenceAuthorisation(
      temporaryAbsenceAuthorisation(
        status = AuthorisationStatus.Code.PENDING,
        repeat = false,
        legacyId = newId(),
        start = LocalDate.now().plusDays(1),
        end = LocalDate.now().plusDays(1),
        schedule = objectMapper.valueToTree(AuthorisationSchedule(LocalTime.of(8, 0), LocalTime.of(10, 0))),
      ),
    )

    val expired = transactionTemplate.execute {
      val toExpire = requireNotNull(findTemporaryAbsenceAuthorisation(auth.id))
      toExpire.expire(ExpireAuthorisation()) { domain, code ->
        requireNotNull(referenceDataRepository.findAll().first { domain.isInstance(it) && it.code == code })
      }
      toExpire
    }!!
    assertThat(expired.status.code).isEqualTo(AuthorisationStatus.Code.EXPIRED.name)

    uasHandler.handle(TemporaryAbsenceAuthorisationExpired(auth.person.identifier, auth.id, DataSource.NOMIS))

    val occurrence = findForAuthorisation(auth.id).single()
    assertThat(occurrence.status.code).isEqualTo(OccurrenceStatus.Code.EXPIRED.name)
    assertThat(occurrence.start.toLocalTime()).isEqualTo(LocalTime.of(8, 0))
    assertThat(occurrence.end.toLocalTime()).isEqualTo(LocalTime.of(10, 0))
    assertThat(occurrence.location).isEqualTo(Location.empty())
  }
}
