package uk.gov.justice.digital.hmpps.externalmovementsapi.sync.write

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.DataSource
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.IdGenerator.newUuid
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.authorisation.TemporaryAbsenceAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.authorisation.TemporaryAbsenceAuthorisationRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.occurrence.TemporaryAbsenceOccurrenceRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.OccurrenceStatusRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.TemporaryAbsenceAuthorisationCancelled
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.TemporaryAbsenceAuthorisationDeferred
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.TemporaryAbsenceAuthorisationDenied
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.TemporaryAbsenceAuthorisationPending
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.DataGenerator.personIdentifier
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class UnapprovedAuthStatusHandlerTest {

  @Mock
  lateinit var authRepository: TemporaryAbsenceAuthorisationRepository

  @Mock
  lateinit var occStatusRepository: OccurrenceStatusRepository

  @Mock
  lateinit var occRepository: TemporaryAbsenceOccurrenceRepository

  @Mock
  lateinit var objectMapper: ObjectMapper

  @InjectMocks
  lateinit var uas: UnapprovedAuthStatusHandler

  @Test
  fun `should ignore unapproved authorisation events with a source of DPS`() {
    val authId = newUuid()
    val personIdentifier = personIdentifier()

    uas.handle(TemporaryAbsenceAuthorisationPending(personIdentifier, authId, DataSource.DPS))
    uas.handle(TemporaryAbsenceAuthorisationDeferred(personIdentifier, authId, DataSource.DPS))
    uas.handle(TemporaryAbsenceAuthorisationCancelled(personIdentifier, authId, DataSource.DPS))
    uas.handle(TemporaryAbsenceAuthorisationDenied(personIdentifier, authId, DataSource.DPS))

    verify(authRepository, never()).findById(authId)
  }

  @Test
  fun `should ignore unapproved auth events for repeats`() {
    val authId = newUuid()
    val personIdentifier = personIdentifier()
    val authorisation = mock<TemporaryAbsenceAuthorisation>()
    whenever(authorisation.repeat).thenReturn(true)
    whenever(authRepository.findById(authId)).thenReturn(Optional.of(authorisation))

    uas.handle(TemporaryAbsenceAuthorisationPending(personIdentifier, authId, DataSource.NOMIS))
    uas.handle(TemporaryAbsenceAuthorisationDeferred(personIdentifier, authId, DataSource.NOMIS))
    uas.handle(TemporaryAbsenceAuthorisationCancelled(personIdentifier, authId, DataSource.NOMIS))
    uas.handle(TemporaryAbsenceAuthorisationDenied(personIdentifier, authId, DataSource.NOMIS))

    verify(occStatusRepository, never()).findAll()
    verify(occRepository, never()).findByAuthorisationId(authId)
  }

  @Test
  fun `should ignore unapproved auth events for authorisations that are approved`() {
    // listens for unapproved event but should ignore if the authorisation is approved at the time of processing
    val authId = newUuid()
    val personIdentifier = personIdentifier()
    val authorisation = mock<TemporaryAbsenceAuthorisation>()
    whenever(authorisation.repeat).thenReturn(true)
    whenever(authRepository.findById(authId)).thenReturn(Optional.of(authorisation))

    uas.handle(TemporaryAbsenceAuthorisationPending(personIdentifier, authId, DataSource.NOMIS))
    uas.handle(TemporaryAbsenceAuthorisationDeferred(personIdentifier, authId, DataSource.NOMIS))
    uas.handle(TemporaryAbsenceAuthorisationCancelled(personIdentifier, authId, DataSource.NOMIS))
    uas.handle(TemporaryAbsenceAuthorisationDenied(personIdentifier, authId, DataSource.NOMIS))

    verify(occStatusRepository, never()).findAll()
    verify(occRepository, never()).findByAuthorisationId(authId)
  }
}
