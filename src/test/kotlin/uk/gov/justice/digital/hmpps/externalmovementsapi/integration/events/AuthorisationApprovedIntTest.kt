package uk.gov.justice.digital.hmpps.externalmovementsapi.integration.events

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.DataSource
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.AuthorisationStatus
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.OccurrenceStatus
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.TemporaryAbsenceAuthorisationApproved
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.IntegrationTest
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceAuthorisationOperations
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceAuthorisationOperations.Companion.temporaryAbsenceAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceOccurrenceOperations
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceOccurrenceOperations.Companion.temporaryAbsenceOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.authorisation.ApproveAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.sync.write.AuthorisationApprovedHandler

class AuthorisationApprovedIntTest(
  @Autowired private val authApprovedHandler: AuthorisationApprovedHandler,
  @Autowired private val taaOperations: TempAbsenceAuthorisationOperations,
  @Autowired private val taoOperations: TempAbsenceOccurrenceOperations,
) : IntegrationTest(),
  TempAbsenceAuthorisationOperations by taaOperations,
  TempAbsenceOccurrenceOperations by taoOperations {

  @Test
  fun `authorisation approved event schedules pending occurrences if source is NOMIS`() {
    val auth =
      givenTemporaryAbsenceAuthorisation(
        temporaryAbsenceAuthorisation(
          status = AuthorisationStatus.Code.PENDING,
          repeat = true,
        ),
      )
    val occurrences = listOf(
      givenTemporaryAbsenceOccurrence(temporaryAbsenceOccurrence(auth)),
      givenTemporaryAbsenceOccurrence(temporaryAbsenceOccurrence(auth)),
    )
    val approved = transactionTemplate.execute {
      val toApprove = requireNotNull(findTemporaryAbsenceAuthorisation(auth.id))
      toApprove.approve(ApproveAuthorisation()) { domain, code ->
        requireNotNull(referenceDataRepository.findAll().first { domain.isInstance(it) && it.code == code })
      }
      toApprove
    }

    assertThat(approved.status.code).isEqualTo(AuthorisationStatus.Code.APPROVED.name)
    occurrences.forEach {
      val refreshed = requireNotNull(findTemporaryAbsenceOccurrence(it.id))
      assertThat(refreshed.status.code).isEqualTo(OccurrenceStatus.Code.PENDING.name)
    }

    authApprovedHandler.handle(TemporaryAbsenceAuthorisationApproved(auth.person.identifier, auth.id, DataSource.NOMIS))

    occurrences.forEach {
      val refreshed = requireNotNull(findTemporaryAbsenceOccurrence(it.id))
      assertThat(refreshed.status.code).isEqualTo(OccurrenceStatus.Code.SCHEDULED.name)
    }
  }
}
