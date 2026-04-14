package uk.gov.justice.digital.hmpps.externalmovementsapi.integration.tap.authorisation

import org.assertj.core.api.Assertions.assertThat
import org.hibernate.envers.RevisionType
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.externalmovementsapi.access.Roles.EXTERNAL_MOVEMENTS_RO
import uk.gov.justice.digital.hmpps.externalmovementsapi.access.Roles.EXTERNAL_MOVEMENTS_UI
import uk.gov.justice.digital.hmpps.externalmovementsapi.access.Roles.TEMPORARY_ABSENCE_RO
import uk.gov.justice.digital.hmpps.externalmovementsapi.access.Roles.TEMPORARY_ABSENCE_RW
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.ExternalMovementContext
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.IdGenerator.newUuid
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.event.producer.publication
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.authorisation.TemporaryAbsenceAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.occurrence.TemporaryAbsenceOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.HmppsDomainEvent
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.TemporaryAbsenceAuthorisationRelocated
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.TemporaryAbsenceRelocated
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.DataGenerator.word
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.IntegrationTest
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.LocationGenerator.location
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceAuthorisationOperations
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceAuthorisationOperations.Companion.temporaryAbsenceAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceOccurrenceOperations
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceOccurrenceOperations.Companion.temporaryAbsenceOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.AuditHistory
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.AuditedAction
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.authorisation.ChangeAuthorisationLocation
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.location.Location
import java.time.LocalDateTime
import java.util.UUID

class ChangeAuthorisationLocationIntTest(
  @Autowired private val taaOperations: TempAbsenceAuthorisationOperations,
  @Autowired private val taoOperations: TempAbsenceOccurrenceOperations,
) : IntegrationTest(),
  TempAbsenceAuthorisationOperations by taaOperations,
  TempAbsenceOccurrenceOperations by taoOperations {

  @Test
  fun `401 unauthorised without a valid token`() {
    webTestClient
      .put()
      .uri(TAP_AUTHORISATION_MODIFICATION_URL, newUuid())
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @ParameterizedTest
  @ValueSource(strings = [TEMPORARY_ABSENCE_RO, EXTERNAL_MOVEMENTS_RO, EXTERNAL_MOVEMENTS_UI])
  fun `403 forbidden without correct role`(role: String) {
    applyLocation(
      UUID.randomUUID(),
      action(),
      role,
    ).expectStatus().isForbidden
  }

  @Test
  fun `404 authorisation does not exist`() {
    applyLocation(newUuid(), action()).expectStatus().isNotFound
  }

  @Test
  fun `200 ok - authorisation location changed`() {
    val location1 = location()
    val location2 = location()
    val auth = givenTemporaryAbsenceAuthorisation(
      temporaryAbsenceAuthorisation(
        repeat = true,
        locations = linkedSetOf(location1, location2),
      ),
    )
    val prev = givenTemporaryAbsenceOccurrence(
      temporaryAbsenceOccurrence(
        auth,
        start = LocalDateTime.now().minusDays(2),
        end = LocalDateTime.now().minusDays(1),
        location = location1,
      ),
    )
    val occ = givenTemporaryAbsenceOccurrence(temporaryAbsenceOccurrence(auth, location = location2))
    val canc = givenTemporaryAbsenceOccurrence(
      temporaryAbsenceOccurrence(
        auth,
        location = location2,
        cancelledAt = LocalDateTime.now().minusDays(1),
        cancelledBy = "C4NC3113D",
      ),
    )

    val request = action()
    val res = applyLocation(auth.id, request).successResponse<AuditHistory>().content.single()
    assertThat(res.domainEvents).containsExactly(TemporaryAbsenceAuthorisationRelocated.EVENT_TYPE)
    assertThat(res.reason).isEqualTo(request.reason)
    assertThat(res.changes).containsExactly(
      AuditedAction.Change(
        "locations",
        auth.locations.map { it.toString() },
        listOf(location1, request.location).map { it.toString() },
      ),
    )

    val saved = requireNotNull(findTemporaryAbsenceAuthorisation(auth.id))
    assertThat(saved.locations).containsExactly(location1, request.location)

    val occurrence = requireNotNull(findTemporaryAbsenceOccurrence(occ.id))
    assertThat(occurrence.location).isEqualTo(request.location)

    val cancelled = requireNotNull(findTemporaryAbsenceOccurrence(canc.id))
    assertThat(cancelled.location).isEqualTo(request.location)

    val previous = requireNotNull(findTemporaryAbsenceOccurrence(prev.id))
    assertThat(previous.location).isEqualTo(location1)

    verifyAudit(
      saved,
      RevisionType.MOD,
      setOf(
        TemporaryAbsenceAuthorisation::class.simpleName!!,
        TemporaryAbsenceOccurrence::class.simpleName!!,
        HmppsDomainEvent::class.simpleName!!,
      ),
      ExternalMovementContext.get().copy(username = DEFAULT_USERNAME, reason = request.reason),
    )

    verifyEventPublications(
      saved,
      setOf(
        TemporaryAbsenceAuthorisationRelocated(auth.person.identifier, auth.id).publication(auth.id),
        TemporaryAbsenceRelocated(auth.person.identifier, occ.id).publication(occ.id),
        TemporaryAbsenceRelocated(auth.person.identifier, canc.id).publication(canc.id),
      ),
    )
  }

  private fun action(
    location: Location = location(),
    reason: String? = (0..5).joinToString(separator = " ") { word(4) },
  ) = ChangeAuthorisationLocation(location, reason)

  private fun applyLocation(
    id: UUID,
    request: ChangeAuthorisationLocation,
    role: String? = TEMPORARY_ABSENCE_RW,
  ) = webTestClient
    .put()
    .uri(TAP_AUTHORISATION_MODIFICATION_URL, id)
    .bodyValue(request)
    .headers(setAuthorisation(username = DEFAULT_USERNAME, roles = listOfNotNull(role)))
    .exchange()

  companion object {
    const val TAP_AUTHORISATION_MODIFICATION_URL = "/temporary-absence-authorisations/{id}"
  }
}
