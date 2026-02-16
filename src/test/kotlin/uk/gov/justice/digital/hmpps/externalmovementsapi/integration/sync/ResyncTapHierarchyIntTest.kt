package uk.gov.justice.digital.hmpps.externalmovementsapi.integration.sync

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.hibernate.envers.RevisionType
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.externalmovementsapi.access.Roles
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.DataSource
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.ExternalMovementContext
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.ExternalMovementContext.Companion.SYSTEM_USERNAME
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.event.producer.publication
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.migration.MigrationSystemAudit
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.migration.MigrationSystemAuditRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.of
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.authorisation.TemporaryAbsenceAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.movement.TemporaryAbsenceMovement
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.movement.TemporaryAbsenceMovement.Direction
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.occurrence.TemporaryAbsenceOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.AuthorisationStatus
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.OccurrenceStatus
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.HmppsDomainEvent
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.TemporaryAbsenceAuthorisationCommentsChanged
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.TemporaryAbsenceAuthorisationDeferred
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.TemporaryAbsenceAuthorisationRelocated
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.DataGenerator.newId
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.DataGenerator.personIdentifier
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.DataGenerator.prisonCode
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.DataGenerator.username
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.DataGenerator.word
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.IntegrationTest
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.LocationGenerator.location
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceAuthorisationOperations
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceAuthorisationOperations.Companion.temporaryAbsenceAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceMovementOperations
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceMovementOperations.Companion.temporaryAbsenceMovement
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceOccurrenceOperations
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceOccurrenceOperations.Companion.temporaryAbsenceOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.wiremock.PrisonerSearchExtension.Companion.prisonerSearch
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.location.Location
import uk.gov.justice.digital.hmpps.externalmovementsapi.sync.AtAndBy
import uk.gov.justice.digital.hmpps.externalmovementsapi.sync.migrate.MigrateTapRequest
import uk.gov.justice.digital.hmpps.externalmovementsapi.sync.migrate.MigrateTapResponse
import uk.gov.justice.digital.hmpps.externalmovementsapi.sync.migrate.TapAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.sync.migrate.TapMovement
import uk.gov.justice.digital.hmpps.externalmovementsapi.sync.migrate.TapOccurrence
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import java.time.temporal.ChronoUnit.SECONDS
import java.util.UUID

class ResyncTapHierarchyIntTest(
  @Autowired private val taaOperations: TempAbsenceAuthorisationOperations,
  @Autowired private val taoOperations: TempAbsenceOccurrenceOperations,
  @Autowired private val tamOperations: TempAbsenceMovementOperations,
  @Autowired private val migrationSystemAuditRepository: MigrationSystemAuditRepository,
) : IntegrationTest(),
  TempAbsenceAuthorisationOperations by taaOperations,
  TempAbsenceOccurrenceOperations by taoOperations,
  TempAbsenceMovementOperations by tamOperations {

  @Test
  fun `401 unauthorised without a valid token`() {
    webTestClient
      .put()
      .uri(RESYNC_TAP_URL, personIdentifier())
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `403 forbidden without correct role`() {
    resyncTap(personIdentifier(), resyncTapRequest(), "ROLE_ANY__OTHER_RW").expectStatus().isForbidden
  }

  @Test
  fun `200 ok - can merge tap for a person deleting and creating new`() {
    val originalAuth = givenTemporaryAbsenceAuthorisation(temporaryAbsenceAuthorisation())
    val originalOcc = givenTemporaryAbsenceOccurrence(
      temporaryAbsenceOccurrence(
        originalAuth,
        legacyId = newId(),
        movements = listOf(
          temporaryAbsenceMovement(
            personIdentifier = originalAuth.person.identifier,
            direction = Direction.OUT,
            legacyId = newId().toString(),
          ),
        ),
      ),
    )
    val um = givenTemporaryAbsenceMovement(temporaryAbsenceMovement(Direction.IN, originalAuth.person.identifier))
    val request = resyncTapRequest(
      temporaryAbsences = listOf(
        tapAuthorisation(occurrences = listOf(tapOccurrence())),
        tapAuthorisation(statusCode = AuthorisationStatus.Code.PENDING.name, occurrences = listOf()),
      ),
    )

    val response = resyncTap(originalAuth.person.identifier, request).successResponse<MigrateTapResponse>()

    // confirm existing tap hierarchy has been removed
    assertThat(findTemporaryAbsenceAuthorisation(originalAuth.id)).isNull()
    assertThat(findTemporaryAbsenceOccurrence(originalOcc.id)).isNull()
    assertThat(findTemporaryAbsenceMovement(originalOcc.movements().first().id)).isNull()

    // confirm unscheduled movements are removed
    assertThat(findTemporaryAbsenceOccurrence(um.id)).isNull()

    response.temporaryAbsences.first().also { ma ->
      val auth = requireNotNull(findTemporaryAbsenceAuthorisation(ma.id))
      val authRequest = request.temporaryAbsences.single { it.legacyId == auth.legacyId }
      val msa = requireNotNull(migrationSystemAuditRepository.findByIdOrNull(auth.id))
      auth.verifyAgainst(auth.person.identifier, authRequest, msa)
      assertThat(auth.reasonPath.path).containsExactly(
        ReferenceDataDomain.Code.ABSENCE_TYPE of "SR",
        ReferenceDataDomain.Code.ABSENCE_SUB_TYPE of "RDR",
        ReferenceDataDomain.Code.ABSENCE_REASON_CATEGORY of "PW",
        ReferenceDataDomain.Code.ABSENCE_REASON of "R15",
      )
      ma.occurrences.forEach { mo ->
        val occurrence = requireNotNull(findTemporaryAbsenceOccurrence(mo.id))
        val occRequest = authRequest.occurrences.single { it.legacyId == occurrence.legacyId }
        val msa = requireNotNull(migrationSystemAuditRepository.findByIdOrNull(occurrence.id))
        occurrence.verifyAgainst(occRequest, msa)
        assertThat(auth.reasonPath.path).containsExactly(
          ReferenceDataDomain.Code.ABSENCE_TYPE of "SR",
          ReferenceDataDomain.Code.ABSENCE_SUB_TYPE of "RDR",
          ReferenceDataDomain.Code.ABSENCE_REASON_CATEGORY of "PW",
          ReferenceDataDomain.Code.ABSENCE_REASON of "R15",
        )
        mo.movements.forEach { mm ->
          val movement = requireNotNull(findTemporaryAbsenceMovement(mm.id))
          val movementRequest = occRequest.movements.single { it.legacyId == movement.legacyId }
          val msa = requireNotNull(migrationSystemAuditRepository.findByIdOrNull(movement.id))
          movement.verifyAgainst(auth.person.identifier, movementRequest, msa)
        }
      }

      verifyAudit(
        auth,
        RevisionType.ADD,
        setOf(
          TemporaryAbsenceAuthorisation::class.simpleName!!,
          TemporaryAbsenceOccurrence::class.simpleName!!,
          TemporaryAbsenceMovement::class.simpleName!!,
          HmppsDomainEvent::class.simpleName!!,
        ),
        ExternalMovementContext.get().copy(username = SYSTEM_USERNAME, source = DataSource.NOMIS, reason = null),
      )
    }

    with(response.temporaryAbsences.last()) {
      val expired = requireNotNull(findTemporaryAbsenceAuthorisation(id))
      assertThat(expired.status.code).isEqualTo(AuthorisationStatus.Code.EXPIRED.name)
    }
  }

  @Test
  fun `200 ok - can migrate tap with partial reason categorisation`() {
    val prisonCode = prisonCode()
    val pi = personIdentifier()
    prisonerSearch.getPrisoners(prisonCode, setOf(pi))
    val request = resyncTapRequest(
      temporaryAbsences = listOf(
        tapAuthorisation(
          typeCode = "PP",
          subTypeCode = "PP",
          reasonCode = "PC",
          occurrences = listOf(
            tapOccurrence(
              typeCode = "PP",
              subTypeCode = "PP",
              reasonCode = "PC",
            ),
          ),
        ),
      ),
      unscheduledMovements = listOf(),
    )
    val response = resyncTap(pi, request).successResponse<MigrateTapResponse>()

    response.temporaryAbsences.first().also { ma ->
      val auth = requireNotNull(findTemporaryAbsenceAuthorisation(ma.id))
      val authRequest = request.temporaryAbsences.single { it.legacyId == auth.legacyId }
      val msa = requireNotNull(migrationSystemAuditRepository.findByIdOrNull(auth.id))
      auth.verifyAgainst(auth.person.identifier, authRequest, msa)
      assertThat(auth.reasonPath.path).containsExactly(ReferenceDataDomain.Code.ABSENCE_TYPE of "PP")
      ma.occurrences.forEach { mo ->
        val occurrence = requireNotNull(findTemporaryAbsenceOccurrence(mo.id))
        val occRequest = authRequest.occurrences.single { it.legacyId == occurrence.legacyId }
        val msa = requireNotNull(migrationSystemAuditRepository.findByIdOrNull(occurrence.id))
        occurrence.verifyAgainst(occRequest, msa)
        assertThat(occurrence.reasonPath.path).containsExactly(ReferenceDataDomain.Code.ABSENCE_TYPE of "PP")
        mo.movements.forEach { mm ->
          val movement = requireNotNull(findTemporaryAbsenceMovement(mm.id))
          val movementRequest = occRequest.movements.single { it.legacyId == movement.legacyId }
          val msa = requireNotNull(migrationSystemAuditRepository.findByIdOrNull(movement.id))
          movement.verifyAgainst(auth.person.identifier, movementRequest, msa)
        }
      }

      verifyAudit(
        auth,
        RevisionType.ADD,
        setOf(
          TemporaryAbsenceAuthorisation::class.simpleName!!,
          TemporaryAbsenceOccurrence::class.simpleName!!,
          TemporaryAbsenceMovement::class.simpleName!!,
          HmppsDomainEvent::class.simpleName!!,
        ),
        ExternalMovementContext.get().copy(username = SYSTEM_USERNAME, source = DataSource.NOMIS),
      )
    }
  }

  @Test
  fun `200 ok - can migrate with reason only categorisation`() {
    val prisonCode = prisonCode()
    val pi = personIdentifier()
    prisonerSearch.getPrisoners(prisonCode, setOf(pi))
    val request = resyncTapRequest(
      temporaryAbsences = listOf(
        tapAuthorisation(
          typeCode = null,
          subTypeCode = null,
          reasonCode = "ET",
          occurrences = listOf(
            tapOccurrence(
              typeCode = null,
              subTypeCode = null,
              reasonCode = "ET",
            ),
          ),
        ),
      ),
      unscheduledMovements = listOf(),
    )
    val response = resyncTap(pi, request).successResponse<MigrateTapResponse>()

    response.temporaryAbsences.first().also { ma ->
      val auth = requireNotNull(findTemporaryAbsenceAuthorisation(ma.id))
      val authRequest = request.temporaryAbsences.single { it.legacyId == auth.legacyId }
      val msa = requireNotNull(migrationSystemAuditRepository.findByIdOrNull(auth.id))
      auth.verifyAgainst(auth.person.identifier, authRequest, msa)
      assertThat(auth.reasonPath.path).containsExactly(ReferenceDataDomain.Code.ABSENCE_REASON_CATEGORY of "ET")
      ma.occurrences.forEach { mo ->
        val occurrence = requireNotNull(findTemporaryAbsenceOccurrence(mo.id))
        val occRequest = authRequest.occurrences.single { it.legacyId == occurrence.legacyId }
        val msa = requireNotNull(migrationSystemAuditRepository.findByIdOrNull(occurrence.id))
        occurrence.verifyAgainst(occRequest, msa)
        assertThat(occurrence.reasonPath.path).containsExactly(ReferenceDataDomain.Code.ABSENCE_REASON_CATEGORY of "ET")
        mo.movements.forEach { mm ->
          val movement = requireNotNull(findTemporaryAbsenceMovement(mm.id))
          val movementRequest = occRequest.movements.single { it.legacyId == movement.legacyId }
          val msa = requireNotNull(migrationSystemAuditRepository.findByIdOrNull(movement.id))
          movement.verifyAgainst(auth.person.identifier, movementRequest, msa)
        }
      }

      verifyAudit(
        auth,
        RevisionType.ADD,
        setOf(
          TemporaryAbsenceAuthorisation::class.simpleName!!,
          TemporaryAbsenceOccurrence::class.simpleName!!,
          TemporaryAbsenceMovement::class.simpleName!!,
          HmppsDomainEvent::class.simpleName!!,
        ),
        ExternalMovementContext.get().copy(username = SYSTEM_USERNAME, source = DataSource.NOMIS),
      )
    }
  }

  @Test
  fun `200 ok - historic absences with movements get correct status`() {
    val prisonCode = prisonCode()
    val pi = personIdentifier()
    prisonerSearch.getPrisoners(prisonCode, setOf(pi))
    val request = resyncTapRequest(
      temporaryAbsences = listOf(
        tapAuthorisation(
          occurrences = listOf(
            tapOccurrence(
              start = LocalDateTime.now().minusDays(3),
              end = LocalDateTime.now().minusDays(2),
              movements = listOf(
                tapMovement(occurredAt = LocalDateTime.now().minusDays(1)),
              ),
            ),
          ),
        ),
        tapAuthorisation(
          occurrences = listOf(
            tapOccurrence(
              start = LocalDateTime.now().minusDays(3),
              end = LocalDateTime.now().minusDays(2),
              movements = listOf(),
            ),
          ),
        ),
      ),
      unscheduledMovements = listOf(),
    )
    val response = resyncTap(pi, request).successResponse<MigrateTapResponse>()

    response.temporaryAbsences.first().also { ma ->
      ma.occurrences.first().also { mo ->
        val occurrence = requireNotNull(findTemporaryAbsenceOccurrence(mo.id))
        assertThat(occurrence.status.code).isEqualTo(OccurrenceStatus.Code.OVERDUE.name)
      }
    }

    response.temporaryAbsences.last().also { ma ->
      ma.occurrences.first().also { mo ->
        val occurrence = requireNotNull(findTemporaryAbsenceOccurrence(mo.id))
        assertThat(occurrence.status.code).isEqualTo(OccurrenceStatus.Code.EXPIRED.name)
      }
    }
  }

  @Test
  fun `200 ok - can make unscheduled movement scheduled`() {
    val auth = givenTemporaryAbsenceAuthorisation(temporaryAbsenceAuthorisation(legacyId = newId()))
    val occurrence = givenTemporaryAbsenceOccurrence(
      temporaryAbsenceOccurrence(
        auth,
        movements = listOf(
          temporaryAbsenceMovement(Direction.OUT, auth.person.identifier, legacyId = newId().toString()),
        ),
        legacyId = newId(),
      ),
    )
    val inMovement = givenTemporaryAbsenceMovement(
      temporaryAbsenceMovement(Direction.IN, auth.person.identifier, legacyId = newId().toString()),
    )

    val request = resyncTapRequest(
      temporaryAbsences = listOf(
        tapAuthorisation(
          id = auth.id,
          legacyId = auth.legacyId!!,
          occurrences = listOf(
            tapOccurrence(
              id = occurrence.id,
              legacyId = occurrence.legacyId!!,
              start = LocalDateTime.now().minusDays(3),
              end = LocalDateTime.now().minusDays(2),
              movements = listOf(
                tapMovement(
                  id = occurrence.movements().first().id,
                  legacyId = occurrence.movements().first().legacyId!!,
                  occurredAt = LocalDateTime.now().minusDays(1),
                  direction = Direction.OUT,
                ),
                tapMovement(
                  id = inMovement.id,
                  legacyId = inMovement.legacyId!!,
                  occurredAt = LocalDateTime.now(),
                  direction = Direction.IN,
                ),
              ),
            ),
          ),
        ),
      ),
      unscheduledMovements = listOf(),
    )
    val response = resyncTap(auth.person.identifier, request).successResponse<MigrateTapResponse>()

    response.temporaryAbsences.first().also { ma ->
      ma.occurrences.first().also { mo ->
        val occurrence = requireNotNull(findTemporaryAbsenceOccurrence(mo.id))
        assertThat(occurrence.status.code).isEqualTo(OccurrenceStatus.Code.COMPLETED.name)
        assertThat(occurrence.movements()).hasSize(2)
      }
    }
  }

  @Test
  fun `200 ok - can make scheduled movement unscheduled`() {
    val auth = givenTemporaryAbsenceAuthorisation(temporaryAbsenceAuthorisation(legacyId = newId()))
    val occurrence = givenTemporaryAbsenceOccurrence(
      temporaryAbsenceOccurrence(
        auth,
        movements = listOf(
          temporaryAbsenceMovement(Direction.OUT, auth.person.identifier, legacyId = newId().toString()),
        ),
        legacyId = newId(),
      ),
    )

    val request = resyncTapRequest(
      temporaryAbsences = listOf(
        tapAuthorisation(
          id = auth.id,
          legacyId = auth.legacyId!!,
          occurrences = listOf(
            tapOccurrence(
              id = occurrence.id,
              legacyId = occurrence.legacyId!!,
              start = LocalDateTime.now().minusDays(3),
              end = LocalDateTime.now().minusDays(2),
              movements = listOf(),
            ),
          ),
        ),
      ),
      unscheduledMovements = listOf(
        occurrence.movements().single().let {
          tapMovement(
            id = it.id,
            prisonCode = it.prisonCode,
            legacyId = it.legacyId!!,
            occurredAt = it.occurredAt,
            direction = it.direction,
            location = it.location,
          )
        },
      ),
    )
    val response = resyncTap(auth.person.identifier, request).successResponse<MigrateTapResponse>()

    response.temporaryAbsences.single().also { ma ->
      ma.occurrences.single().also { mo ->
        val occurrence = requireNotNull(findTemporaryAbsenceOccurrence(mo.id))
        assertThat(occurrence.status.code).isEqualTo(OccurrenceStatus.Code.EXPIRED.name)
        assertThat(occurrence.movements()).hasSize(0)
      }
    }
  }

  @Test
  fun `200 ok - can remove occurrence`() {
    val auth = givenTemporaryAbsenceAuthorisation(temporaryAbsenceAuthorisation(legacyId = newId()))
    val occurrence = givenTemporaryAbsenceOccurrence(
      temporaryAbsenceOccurrence(
        auth,
        movements = listOf(
          temporaryAbsenceMovement(Direction.OUT, auth.person.identifier, legacyId = newId().toString()),
        ),
        legacyId = newId(),
      ),
    )

    val request = resyncTapRequest(
      temporaryAbsences = listOf(
        tapAuthorisation(
          id = auth.id,
          legacyId = auth.legacyId!!,
          occurrences = listOf(),
        ),
      ),
      unscheduledMovements = listOf(
        occurrence.movements().single().let {
          tapMovement(
            id = it.id,
            prisonCode = it.prisonCode,
            legacyId = it.legacyId!!,
            occurredAt = it.occurredAt,
            direction = it.direction,
            location = it.location,
          )
        },
      ),
    )
    val response = resyncTap(auth.person.identifier, request).successResponse<MigrateTapResponse>()

    response.temporaryAbsences.single().also { ma -> assertThat(ma.occurrences.isEmpty()) }
    assertThat(findTemporaryAbsenceOccurrence(occurrence.id)).isNull()
    response.unscheduledMovements.single().also { mo ->
      val movement = requireNotNull(findTemporaryAbsenceMovement(mo.id))
      val msa = requireNotNull(migrationSystemAuditRepository.findByIdOrNull(movement.id))
      movement.verifyAgainst(auth.person.identifier, request.unscheduledMovements.single(), msa)
    }
  }

  @Test
  fun `200 ok - can add new occurrences`() {
    val auth = givenTemporaryAbsenceAuthorisation(temporaryAbsenceAuthorisation(legacyId = newId()))
    val occurrence = givenTemporaryAbsenceOccurrence(
      temporaryAbsenceOccurrence(
        auth,
        movements = listOf(
          temporaryAbsenceMovement(Direction.OUT, auth.person.identifier, legacyId = newId().toString()),
        ),
        legacyId = newId(),
      ),
    )

    val request = resyncTapRequest(
      temporaryAbsences = listOf(
        tapAuthorisation(
          id = auth.id,
          legacyId = auth.legacyId!!,
          occurrences = listOf(
            tapOccurrence(id = occurrence.id, legacyId = occurrence.legacyId!!),
            tapOccurrence(id = null, legacyId = newId()),
          ),
        ),
      ),
      unscheduledMovements = listOf(),
    )
    val response = resyncTap(auth.person.identifier, request).successResponse<MigrateTapResponse>()

    val responseAuth = response.temporaryAbsences.single()
    assertThat(responseAuth.occurrences).hasSize(2)
    responseAuth.occurrences.forEach { occ ->
      val occurrence = requireNotNull(findTemporaryAbsenceOccurrence(occ.id))
      val occRequest =
        request.temporaryAbsences.flatMap { it.occurrences }.single { it.legacyId == occurrence.legacyId }
      val msa = requireNotNull(migrationSystemAuditRepository.findByIdOrNull(occurrence.id))
      occurrence.verifyAgainst(occRequest, msa)
    }
  }

  @Test
  fun `200 ok - resync of expired authorisation maintains status`() {
    val auth = givenTemporaryAbsenceAuthorisation(
      temporaryAbsenceAuthorisation(
        start = LocalDate.now().minusDays(10),
        end = LocalDate.now().minusDays(5),
        status = AuthorisationStatus.Code.EXPIRED,
        locations = linkedSetOf(location()),
        legacyId = newId(),
      ),
    )

    val request = resyncTapRequest(
      temporaryAbsences = listOf(
        tapAuthorisation(
          id = auth.id,
          legacyId = auth.legacyId!!,
          occurrences = listOf(),
          // nomis doesn't have the concept of expired so sends pending
          statusCode = "PENDING",
          start = auth.start,
          end = auth.end,
          location = auth.locations.single(),
          comments = "Resync authorisation",
        ),
      ),
      unscheduledMovements = listOf(),
    )
    resyncTap(auth.person.identifier, request).successResponse<MigrateTapResponse>()

    val saved = requireNotNull(findTemporaryAbsenceAuthorisation(auth.id))
    assertThat(saved.status.code).isEqualTo(AuthorisationStatus.Code.EXPIRED.name)

    verifyAudit(
      saved,
      RevisionType.MOD,
      setOf(TemporaryAbsenceAuthorisation::class.simpleName!!, HmppsDomainEvent::class.simpleName!!),
      ExternalMovementContext.get().copy(source = DataSource.NOMIS),
    )

    verifyEventPublications(
      saved,
      setOf(
        TemporaryAbsenceAuthorisationCommentsChanged(auth.person.identifier, auth.id, DataSource.NOMIS).publication(auth.id),
      ),
    )
  }

  @Test
  fun `200 ok - can reset to pending if not in the past`() {
    val auth = givenTemporaryAbsenceAuthorisation(
      temporaryAbsenceAuthorisation(
        start = LocalDate.now().minusDays(3),
        end = LocalDate.now().plusDays(3),
        status = AuthorisationStatus.Code.APPROVED,
        locations = linkedSetOf(location()),
        legacyId = newId(),
      ),
    )

    val request = resyncTapRequest(
      temporaryAbsences = listOf(
        tapAuthorisation(
          id = auth.id,
          legacyId = auth.legacyId!!,
          occurrences = listOf(),
          statusCode = "PENDING",
          start = auth.start,
          end = auth.end,
          location = location(),
          comments = "Resync authorisation",
        ),
      ),
      unscheduledMovements = listOf(),
    )
    resyncTap(auth.person.identifier, request).successResponse<MigrateTapResponse>()

    val saved = requireNotNull(findTemporaryAbsenceAuthorisation(auth.id))
    assertThat(saved.status.code).isEqualTo(AuthorisationStatus.Code.PENDING.name)

    verifyAudit(
      saved,
      RevisionType.MOD,
      setOf(TemporaryAbsenceAuthorisation::class.simpleName!!, HmppsDomainEvent::class.simpleName!!),
      ExternalMovementContext.get().copy(source = DataSource.NOMIS),
    )

    verifyEventPublications(
      saved,
      setOf(
        TemporaryAbsenceAuthorisationCommentsChanged(auth.person.identifier, auth.id, DataSource.NOMIS).publication(auth.id),
        TemporaryAbsenceAuthorisationRelocated(auth.person.identifier, auth.id, DataSource.NOMIS).publication(auth.id),
        TemporaryAbsenceAuthorisationDeferred(auth.person.identifier, auth.id, DataSource.NOMIS).publication(auth.id),
      ),
    )
  }

  private fun resyncTapRequest(
    temporaryAbsences: List<TapAuthorisation> = listOf(tapAuthorisation()),
    unscheduledMovements: List<TapMovement> = listOf(tapMovement()),
  ) = MigrateTapRequest(temporaryAbsences, unscheduledMovements)

  private fun tapAuthorisation(
    prisonCode: String = prisonCode(),
    reasonCode: String = "R15",
    typeCode: String? = "SR",
    subTypeCode: String? = "RDR",
    statusCode: String = "APPROVED",
    accompaniedByCode: String = "L",
    transportCode: String = "OD",
    repeat: Boolean = false,
    comments: String? = "Some comments about the application",
    start: LocalDate = LocalDate.now().minusDays(7),
    end: LocalDate = LocalDate.now().minusDays(1),
    startTime: LocalTime = LocalTime.now().plusMinutes(10).truncatedTo(ChronoUnit.MINUTES),
    endTime: LocalTime = LocalDateTime.of(start, startTime).plusHours(2).toLocalTime().truncatedTo(ChronoUnit.MINUTES),
    location: Location = location(),
    created: AtAndBy = AtAndBy(LocalDateTime.now().minusHours(1), username()),
    updated: AtAndBy? = AtAndBy(LocalDateTime.now().minusHours(1), username()),
    legacyId: Long = newId(),
    id: UUID? = null,
    occurrences: List<TapOccurrence> = listOf(tapOccurrence()),
  ) = TapAuthorisation(
    prisonCode,
    statusCode,
    typeCode,
    subTypeCode,
    reasonCode,
    accompaniedByCode,
    transportCode,
    repeat,
    start,
    end,
    startTime,
    endTime,
    location,
    comments,
    created,
    updated,
    legacyId,
    id,
    occurrences,
  )

  private fun tapOccurrence(
    isCancelled: Boolean = false,
    reasonCode: String = "R15",
    typeCode: String? = "SR",
    subTypeCode: String? = "RDR",
    start: LocalDateTime = LocalDateTime.now().minusHours(7),
    end: LocalDateTime = LocalDateTime.now().plusHours(1),
    accompaniedByCode: String = "L",
    transportCode: String = "OD",
    location: Location = location(),
    contactInformation: String? = "Contact ${word(8)}",
    comments: String? = "Some comments about the absence",
    created: AtAndBy = AtAndBy(LocalDateTime.now().minusMonths(1), username()),
    updated: AtAndBy? = AtAndBy(LocalDateTime.now().minusWeeks(1), username()),
    legacyId: Long = newId(),
    id: UUID? = null,
    movements: List<TapMovement> = listOf(tapMovement()),
  ) = TapOccurrence(
    isCancelled,
    start,
    end,
    location,
    typeCode,
    subTypeCode,
    reasonCode,
    accompaniedByCode,
    transportCode,
    contactInformation,
    comments,
    created,
    updated,
    legacyId,
    id,
    movements,
  )

  private fun tapMovement(
    direction: Direction = Direction.OUT,
    prisonCode: String = prisonCode(),
    occurredAt: LocalDateTime = LocalDateTime.now().minusDays(7),
    reasonCode: String = "R15",
    accompaniedByCode: String = "L",
    accompaniedByComments: String? = "Information about the escort",
    comments: String? = "Some comments about the movement",
    location: Location = location(),
    legacyId: String = "${newId()}",
    id: UUID? = null,
    recordedBy: String = username(),
    recordedAt: LocalDateTime = LocalDateTime.now().minusDays(7),
  ) = TapMovement(
    occurredAt,
    direction,
    prisonCode,
    reasonCode,
    location,
    accompaniedByCode,
    accompaniedByComments,
    comments,
    AtAndBy(recordedAt, recordedBy),
    null,
    legacyId,
    id,
  )

  private fun resyncTap(
    personIdentifier: String,
    request: MigrateTapRequest,
    role: String? = Roles.NOMIS_SYNC,
  ) = webTestClient
    .put()
    .uri(RESYNC_TAP_URL, personIdentifier)
    .bodyValue(request)
    .headers(setAuthorisation(username = "migration-client-id", roles = listOfNotNull(role)))
    .exchange()

  companion object {
    const val RESYNC_TAP_URL = "/resync/temporary-absences/{personIdentifier}"

    private fun TemporaryAbsenceAuthorisation.verifyAgainst(
      personIdentifier: String,
      request: TapAuthorisation,
      msa: MigrationSystemAudit,
    ) {
      assertThat(person.identifier).isEqualTo(personIdentifier)
      assertThat(legacyId).isEqualTo(request.legacyId)
      assertThat(status.code).isEqualTo(request.statusCode)
      assertThat(absenceType?.code).isEqualTo(request.absenceTypeCode)
      assertThat(absenceSubType?.code).isEqualTo(request.absenceSubTypeCode)
      assertThat(absenceReason.code).isEqualTo(request.absenceReasonCode)
      assertThat(accompaniedBy.code).isEqualTo(request.accompaniedByCode)
      assertThat(prisonCode).isEqualTo(request.prisonCode)
      assertThat(repeat).isEqualTo(request.repeat)
      assertThat(comments).isEqualTo(request.comments)
      assertThat(start).isEqualTo(request.start)
      assertThat(end).isEqualTo(request.end)
      assertThat(msa.createdBy).isEqualTo(request.created.by)
      assertThat(msa.createdAt).isCloseTo(request.created.at, within(1, SECONDS))
      request.updated?.also {
        assertThat(msa.updatedBy).isEqualTo(it.by)
        assertThat(msa.updatedAt).isCloseTo(it.at, within(1, SECONDS))
      }
    }

    private fun TemporaryAbsenceOccurrence.verifyAgainst(request: TapOccurrence, msa: MigrationSystemAudit) {
      assertThat(start).isCloseTo(request.start, within(2, SECONDS))
      assertThat(end).isCloseTo(request.end, within(2, SECONDS))
      assertThat(accompaniedBy.code).isEqualTo(request.accompaniedByCode)
      assertThat(transport.code).isEqualTo(request.transportCode)
      assertThat(location).isEqualTo(request.location)
      assertThat(contactInformation).isEqualTo(request.contactInformation)
      assertThat(comments).isEqualTo(request.comments)
      assertThat(legacyId).isEqualTo(request.legacyId)
      assertThat(msa.createdBy).isEqualTo(request.created.by)
      assertThat(msa.createdAt).isCloseTo(request.created.at, within(1, SECONDS))
      request.updated?.also {
        assertThat(msa.updatedBy).isEqualTo(it.by)
        assertThat(msa.updatedAt).isCloseTo(it.at, within(1, SECONDS))
      }
    }

    private fun TemporaryAbsenceMovement.verifyAgainst(
      personIdentifier: String,
      request: TapMovement,
      msa: MigrationSystemAudit,
    ) {
      assertThat(this.person.identifier).isEqualTo(personIdentifier)
      assertThat(direction.name).isEqualTo(request.direction.name)
      assertThat(occurredAt).isCloseTo(request.occurredAt, within(2, SECONDS))
      assertThat(absenceReason.code).isEqualTo(request.absenceReasonCode)
      assertThat(accompaniedBy.code).isEqualTo(request.accompaniedByCode)
      assertThat(location).isEqualTo(request.location)
      assertThat(comments).isEqualTo(request.comments)
      assertThat(prisonCode).isEqualTo(request.prisonCode)
      assertThat(legacyId).isEqualTo(request.legacyId)
      assertThat(msa.createdBy).isEqualTo(request.created.by)
      assertThat(msa.createdAt).isCloseTo(request.created.at, within(1, SECONDS))
      request.updated?.also {
        assertThat(msa.updatedBy).isEqualTo(it.by)
        assertThat(msa.updatedAt).isCloseTo(it.at, within(1, SECONDS))
      }
    }
  }
}
