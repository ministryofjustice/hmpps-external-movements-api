package uk.gov.justice.digital.hmpps.externalmovementsapi.sync.write

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
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.TemporaryAbsenceAuthorisationApproved
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.DataGenerator.personIdentifier
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class AuthorisationApprovedHandlerTest {

  @Mock
  lateinit var authRepository: TemporaryAbsenceAuthorisationRepository

  @Mock
  lateinit var occStatusRepository: OccurrenceStatusRepository

  @Mock
  lateinit var occRepository: TemporaryAbsenceOccurrenceRepository

  @InjectMocks
  lateinit var authorisationApprovedHandler: AuthorisationApprovedHandler

  @Test
  fun `should ignore auth approved events with source of DPS`() {
    val authId = newUuid()
    val personIdentifier = personIdentifier()

    authorisationApprovedHandler.handle(TemporaryAbsenceAuthorisationApproved(personIdentifier, authId, DataSource.DPS))

    verify(authRepository, never()).findById(authId)
  }

  @Test
  fun `should ignore auth approved events for singles`() {
    val authId = newUuid()
    val personIdentifier = personIdentifier()
    val authorisation = mock<TemporaryAbsenceAuthorisation>()
    whenever(authorisation.repeat).thenReturn(false)
    whenever(authRepository.findById(authId)).thenReturn(Optional.of(authorisation))

    authorisationApprovedHandler.handle(TemporaryAbsenceAuthorisationApproved(personIdentifier, authId, DataSource.NOMIS))

    verify(occRepository, never()).findByAuthorisationId(authId)
  }
}
