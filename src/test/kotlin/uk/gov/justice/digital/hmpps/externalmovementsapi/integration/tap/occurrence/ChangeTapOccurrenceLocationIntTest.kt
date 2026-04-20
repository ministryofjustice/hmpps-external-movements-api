package uk.gov.justice.digital.hmpps.externalmovementsapi.integration.tap.occurrence

import org.assertj.core.api.Assertions.assertThat
import org.hibernate.envers.RevisionType
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.externalmovementsapi.access.Roles
import uk.gov.justice.digital.hmpps.externalmovementsapi.access.Roles.EXTERNAL_MOVEMENTS_RO
import uk.gov.justice.digital.hmpps.externalmovementsapi.access.Roles.EXTERNAL_MOVEMENTS_UI
import uk.gov.justice.digital.hmpps.externalmovementsapi.access.Roles.TEMPORARY_ABSENCE_RO
import uk.gov.justice.digital.hmpps.externalmovementsapi.config.CaseloadIdHeader
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.ExternalMovementContext
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.IdGenerator.newUuid
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.event.producer.publication
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.authorisation.TemporaryAbsenceAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.occurrence.TemporaryAbsenceOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.HmppsDomainEvent
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.TemporaryAbsenceAuthorisationRelocated
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.TemporaryAbsenceRelocated
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.DataGenerator.username
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.DataGenerator.word
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.IntegrationTest
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.LocationGenerator.location
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceAuthorisationOperations
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceAuthorisationOperations.Companion.temporaryAbsenceAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceOccurrenceOperations
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceOccurrenceOperations.Companion.temporaryAbsenceOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.AuditHistory
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.AuditedAction
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.occurrence.ChangeOccurrenceLocation
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.location.Location
import java.util.UUID

class ChangeTapOccurrenceLocationIntTest(
  @Autowired private val taaOperations: TempAbsenceAuthorisationOperations,
  @Autowired private val taoOperations: TempAbsenceOccurrenceOperations,
) : IntegrationTest(),
  TempAbsenceAuthorisationOperations by taaOperations,
  TempAbsenceOccurrenceOperations by taoOperations {

  @Test
  fun `401 unauthorised without a valid token`() {
    webTestClient
      .put()
      .uri(TAP_OCCURRENCE_MODIFICATION_URL, newUuid())
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @ParameterizedTest
  @ValueSource(strings = [TEMPORARY_ABSENCE_RO, EXTERNAL_MOVEMENTS_RO, EXTERNAL_MOVEMENTS_UI])
  fun `403 forbidden without correct role`(role: String) {
    applyLocation(
      newUuid(),
      action(),
      role = role,
    ).expectStatus().isForbidden
  }

  @Test
  fun `404 occurrence does not exist`() {
    applyLocation(newUuid(), action()).expectStatus().isNotFound
  }

  @Test
  fun `400 bad request - description or address part required`() {
    val res = applyLocation(newUuid(), action(location(description = null, address = null, postcode = null)))
      .errorResponse(HttpStatus.BAD_REQUEST)

    assertThat(res.status).isEqualTo(HttpStatus.BAD_REQUEST.value())
    assertThat(res.userMessage).isEqualTo("Validation failure: Either a description or partial address must be specified.")
  }

  @Test
  fun `200 ok tap occurrence location updated successfully`() {
    val auth = givenTemporaryAbsenceAuthorisation(temporaryAbsenceAuthorisation())
    val occurrence = givenTemporaryAbsenceOccurrence(temporaryAbsenceOccurrence(auth))
    val request = action()
    val username = username()
    val caseloadId = word(3)
    val res = applyLocation(occurrence.id, request, username, caseloadId).successResponse<AuditHistory>().content.single()
    assertThat(res.domainEvents).containsExactly(TemporaryAbsenceRelocated.EVENT_TYPE)
    assertThat(res.reason).isEqualTo(request.reason)
    assertThat(res.changes).containsExactly(
      AuditedAction.Change(
        "location",
        occurrence.location.toString(),
        request.location.toString(),
      ),
    )

    val saved = requireNotNull(findTemporaryAbsenceOccurrence(occurrence.id))
    assertThat(saved.location).isEqualTo(request.location)

    verifyAudit(
      saved,
      RevisionType.MOD,
      setOf(
        TemporaryAbsenceOccurrence::class.simpleName!!,
        TemporaryAbsenceAuthorisation::class.simpleName!!,
        HmppsDomainEvent::class.simpleName!!,
      ),
      ExternalMovementContext.get().copy(username = username, reason = request.reason, caseloadId = caseloadId),
    )

    verifyEventPublications(
      saved,
      setOf(
        TemporaryAbsenceRelocated(auth.person.identifier, occurrence.id).publication(occurrence.id),
        TemporaryAbsenceAuthorisationRelocated(auth.person.identifier, auth.id).publication(auth.id),
      ),
    )
  }

  @Test
  fun `200 ok tap occurrence location for repeat updated successfully`() {
    val auth = givenTemporaryAbsenceAuthorisation(temporaryAbsenceAuthorisation(repeat = true, locations = linkedSetOf(location())))
    val occ1 = givenTemporaryAbsenceOccurrence(temporaryAbsenceOccurrence(auth, location = auth.locations.first))
    val occ2 = givenTemporaryAbsenceOccurrence(temporaryAbsenceOccurrence(auth, location = auth.locations.first))
    val request = action()
    val res = applyLocation(occ1.id, request, caseloadId = auth.prisonCode).successResponse<AuditHistory>().content.single()
    assertThat(res.domainEvents).containsExactly(TemporaryAbsenceRelocated.EVENT_TYPE)
    assertThat(res.reason).isEqualTo(request.reason)
    assertThat(res.changes).containsExactly(
      AuditedAction.Change(
        "location",
        occ1.location.toString(),
        request.location.toString(),
      ),
    )

    val saved = requireNotNull(findTemporaryAbsenceOccurrence(occ1.id))
    assertThat(saved.location).isEqualTo(request.location)

    val updatedAuth = requireNotNull(findTemporaryAbsenceAuthorisation(auth.id))
    assertThat(updatedAuth.locations).containsExactly(occ2.location, request.location)

    verifyAudit(
      saved,
      RevisionType.MOD,
      setOf(
        TemporaryAbsenceOccurrence::class.simpleName!!,
        TemporaryAbsenceAuthorisation::class.simpleName!!,
        HmppsDomainEvent::class.simpleName!!,
      ),
      ExternalMovementContext.get().copy(username = DEFAULT_USERNAME, reason = request.reason, caseloadId = auth.prisonCode),
    )

    verifyEventPublications(
      saved,
      setOf(
        TemporaryAbsenceRelocated(auth.person.identifier, occ1.id).publication(occ1.id),
        TemporaryAbsenceAuthorisationRelocated(auth.person.identifier, auth.id).publication(auth.id),
      ),
    )
  }

  private fun action(
    location: Location = location(),
    reason: String? = (0..5).joinToString(separator = " ") { word(4) },
  ) = ChangeOccurrenceLocation(location, reason)

  private fun applyLocation(
    id: UUID,
    request: ChangeOccurrenceLocation,
    username: String = DEFAULT_USERNAME,
    caseloadId: String? = null,
    role: String? = Roles.TEMPORARY_ABSENCE_RW,
  ) = webTestClient
    .put()
    .uri(TAP_OCCURRENCE_MODIFICATION_URL, id)
    .bodyValue(request)
    .headers(setAuthorisation(username = username, roles = listOfNotNull(role)))
    .headers { h -> caseloadId?.also { h.put(CaseloadIdHeader.NAME, listOf(it)) } }
    .exchange()

  companion object {
    const val TAP_OCCURRENCE_MODIFICATION_URL = "/temporary-absence-occurrences/{id}"
  }
}
