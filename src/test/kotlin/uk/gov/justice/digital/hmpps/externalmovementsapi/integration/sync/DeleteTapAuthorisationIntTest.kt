package uk.gov.justice.digital.hmpps.externalmovementsapi.integration.sync

import org.assertj.core.api.Assertions.assertThat
import org.hibernate.envers.RevisionType
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.externalmovementsapi.access.Roles
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.DataSource
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.ExternalMovementContext
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.ExternalMovementContext.Companion.SYSTEM_USERNAME
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.IdGenerator.newUuid
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.authorisation.TemporaryAbsenceAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.occurrence.TemporaryAbsenceOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.IntegrationTest
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceAuthorisationOperations
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceAuthorisationOperations.Companion.temporaryAbsenceAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceOccurrenceOperations
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceOccurrenceOperations.Companion.temporaryAbsenceOccurrence
import java.util.UUID

class DeleteTapAuthorisationIntTest(
  @Autowired private val taaOperations: TempAbsenceAuthorisationOperations,
  @Autowired private val taoOperations: TempAbsenceOccurrenceOperations,
) : IntegrationTest(),
  TempAbsenceAuthorisationOperations by taaOperations,
  TempAbsenceOccurrenceOperations by taoOperations {

  @Test
  fun `401 unauthorised without a valid token`() {
    webTestClient
      .delete()
      .uri(DELETE_TAP_AUTH_URL, newUuid())
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `403 forbidden without correct role`() {
    deleteTapAuthorisation(newUuid(), "ROLE_ANY__OTHER_RW").expectStatus().isForbidden
  }

  @Test
  fun `204 no content when id invalid or already deleted`() {
    deleteTapAuthorisation(newUuid()).expectStatus().isNoContent
  }

  @Test
  fun `204 can delete tap authorisation and occurrences`() {
    val auth = givenTemporaryAbsenceAuthorisation(temporaryAbsenceAuthorisation())
    givenTemporaryAbsenceOccurrence(temporaryAbsenceOccurrence(auth))

    deleteTapAuthorisation(auth.id).expectStatus().isNoContent

    val saved = findTemporaryAbsenceAuthorisation(auth.id)
    assertThat(saved).isNull()

    verifyAudit(
      auth,
      RevisionType.DEL,
      setOf(TemporaryAbsenceOccurrence::class.simpleName!!, TemporaryAbsenceAuthorisation::class.simpleName!!),
      ExternalMovementContext.get().copy(source = DataSource.NOMIS),
    )

    verifyEvents(auth, setOf())
  }

  private fun deleteTapAuthorisation(
    id: UUID,
    role: String? = Roles.NOMIS_SYNC,
  ) = webTestClient
    .delete()
    .uri(DELETE_TAP_AUTH_URL, id)
    .headers(setAuthorisation(username = SYSTEM_USERNAME, roles = listOfNotNull(role)))
    .exchange()

  companion object {
    const val DELETE_TAP_AUTH_URL = "/sync/temporary-absence-authorisations/{id}"
  }
}
