package uk.gov.justice.digital.hmpps.externalmovementsapi.integration.sync

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.hibernate.envers.AuditReaderFactory
import org.hibernate.envers.RevisionType
import org.hibernate.envers.query.AuditEntity.revisionNumber
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.externalmovementsapi.access.Roles
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.DataSource
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.ExternalMovementContext
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.ExternalMovementContext.Companion.SYSTEM_USERNAME
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.Identifiable
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.authorisation.TemporaryAbsenceAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.movement.TemporaryAbsenceMovement
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.movement.TemporaryAbsenceMovement.Direction
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.occurrence.TemporaryAbsenceOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.migration.MigrationSystemAudit
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.migration.MigrationSystemAuditRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.TapAuthorisationStatus
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.TapOccurrenceStatus
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.of
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.HmppsDomainEvent
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.DataGenerator.newId
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.DataGenerator.personIdentifier
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.DataGenerator.prisonCode
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.DataGenerator.username
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.DataGenerator.word
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.IntegrationTest
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceAuthorisationOperations
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceAuthorisationOperations.Companion.temporaryAbsenceAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceMovementOperations
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceMovementOperations.Companion.temporaryAbsenceMovement
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceOccurrenceOperations
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceOccurrenceOperations.Companion.location
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceOccurrenceOperations.Companion.temporaryAbsenceOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.wiremock.PrisonerSearchExtension.Companion.prisonerSearch
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.location.Location
import uk.gov.justice.digital.hmpps.externalmovementsapi.sync.AtAndBy
import uk.gov.justice.digital.hmpps.externalmovementsapi.sync.AtAndByWithPrison
import uk.gov.justice.digital.hmpps.externalmovementsapi.sync.migrate.MigrateTapRequest
import uk.gov.justice.digital.hmpps.externalmovementsapi.sync.migrate.MigrateTapResponse
import uk.gov.justice.digital.hmpps.externalmovementsapi.sync.migrate.TapAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.sync.migrate.TapMovement
import uk.gov.justice.digital.hmpps.externalmovementsapi.sync.migrate.TapOccurrence
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit.SECONDS

class MigrateTapHierarchyIntTest(
  @Autowired private val taaOperations: TempAbsenceAuthorisationOperations,
  @Autowired private val taoOperations: TempAbsenceOccurrenceOperations,
  @Autowired private val tamOperations: TempAbsenceMovementOperations,
  @Autowired private val msaRepository: MigrationSystemAuditRepository,
) : IntegrationTest(),
  TempAbsenceAuthorisationOperations by taaOperations,
  TempAbsenceOccurrenceOperations by taoOperations,
  TempAbsenceMovementOperations by tamOperations {

  @Test
  fun `401 unauthorised without a valid token`() {
    webTestClient
      .put()
      .uri(MIGRATE_TAP_URL, personIdentifier())
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `403 forbidden without correct role`() {
    migrateTap(personIdentifier(), migrateTapRequest(), "ROLE_ANY__OTHER_RW").expectStatus().isForbidden
  }

  @Test
  fun `200 ok - can migrate tap for a person`() {
    val auth = givenTemporaryAbsenceAuthorisation(temporaryAbsenceAuthorisation(legacyId = newId()))
    val occurrence = givenTemporaryAbsenceOccurrence(
      temporaryAbsenceOccurrence(
        auth,
        legacyId = newId(),
        movements = listOf(
          temporaryAbsenceMovement(
            personIdentifier = auth.person.identifier,
            direction = Direction.OUT,
            legacyId = newId().toString(),
          ),
        ),
      ),
    )
    val um = givenTemporaryAbsenceMovement(temporaryAbsenceMovement(Direction.IN, auth.person.identifier))
    prisonerSearch.getPrisoners(auth.prisonCode, setOf(auth.person.identifier))
    val request = migrateTapRequest(
      temporaryAbsences = listOf(
        tapAuthorisation(),
        tapAuthorisation(statusCode = TapAuthorisationStatus.Code.PENDING.name, occurrences = listOf()),
      ),
    )
    val response = migrateTap(auth.person.identifier, request).successResponse<MigrateTapResponse>()

    // confirm existing tap hierarchy has been removed
    assertThat(findTemporaryAbsenceAuthorisation(auth.id)).isNull()
    assertThat(findTemporaryAbsenceOccurrence(occurrence.id)).isNull()
    assertThat(findTemporaryAbsenceMovement(occurrence.movements().first().id)).isNull()

    // confirm unscheduled movements are removed
    assertThat(findTemporaryAbsenceOccurrence(um.id)).isNull()

    response.temporaryAbsences.first().also { ma ->
      val auth = requireNotNull(findTemporaryAbsenceAuthorisation(ma.id))
      val authRequest = request.temporaryAbsences.single { it.legacyId == auth.legacyId }
      val amsa = requireNotNull(msaRepository.findByIdOrNull(auth.id))
      auth.verifyAgainst(auth.person.identifier, authRequest, amsa)
      assertThat(auth.reasonPath.path).containsExactly(
        ReferenceDataDomain.Code.ABSENCE_TYPE of "SR",
        ReferenceDataDomain.Code.ABSENCE_SUB_TYPE of "RDR",
        ReferenceDataDomain.Code.ABSENCE_REASON_CATEGORY of "PW",
        ReferenceDataDomain.Code.ABSENCE_REASON of "R15",
      )
      ma.occurrences.forEach { mo ->
        val occurrence = requireNotNull(findTemporaryAbsenceOccurrence(mo.id))
        val occRequest = authRequest.occurrences.single { it.legacyId == occurrence.legacyId }
        val omsa = requireNotNull(msaRepository.findByIdOrNull(occurrence.id))
        occurrence.verifyAgainst(occRequest, omsa)
        assertThat(auth.reasonPath.path).containsExactly(
          ReferenceDataDomain.Code.ABSENCE_TYPE of "SR",
          ReferenceDataDomain.Code.ABSENCE_SUB_TYPE of "RDR",
          ReferenceDataDomain.Code.ABSENCE_REASON_CATEGORY of "PW",
          ReferenceDataDomain.Code.ABSENCE_REASON of "R15",
        )
        mo.movements.forEach { mm ->
          val movement = requireNotNull(findTemporaryAbsenceMovement(mm.id))
          val movementRequest = occRequest.movements.single { it.legacyId == movement.legacyId }
          val mmsa = requireNotNull(msaRepository.findByIdOrNull(movement.id))
          movement.verifyAgainst(auth.person.identifier, movementRequest, mmsa)
        }
      }

      verifyAudit(
        auth,
        RevisionType.DEL,
        setOf(
          TemporaryAbsenceAuthorisation::class.simpleName!!,
          TemporaryAbsenceOccurrence::class.simpleName!!,
          TemporaryAbsenceMovement::class.simpleName!!,
          HmppsDomainEvent::class.simpleName!!,
        ),
        ExternalMovementContext.get().copy(username = SYSTEM_USERNAME, source = DataSource.NOMIS),
      )

      val domainEvents = latestRevisionDomainEvents(auth)
      assertThat(domainEvents).hasSize(5)
      domainEvents.forEach { domainEvent -> assertThat(domainEvent.published).isTrue }
    }

    with(response.temporaryAbsences.last()) {
      val expired = requireNotNull(findTemporaryAbsenceAuthorisation(id))
      assertThat(expired.status.code).isEqualTo(TapAuthorisationStatus.Code.EXPIRED.name)
    }
  }

  @Test
  fun `200 ok - can migrate tap with partial reason categorisation`() {
    val prisonCode = prisonCode()
    val pi = personIdentifier()
    prisonerSearch.getPrisoners(prisonCode, setOf(pi))
    val request = migrateTapRequest(
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
    val response = migrateTap(pi, request).successResponse<MigrateTapResponse>()

    response.temporaryAbsences.first().also { ma ->
      val auth = requireNotNull(findTemporaryAbsenceAuthorisation(ma.id))
      val authRequest = request.temporaryAbsences.single { it.legacyId == auth.legacyId }
      val amsa = requireNotNull(msaRepository.findByIdOrNull(auth.id))
      auth.verifyAgainst(auth.person.identifier, authRequest, amsa)
      assertThat(auth.reasonPath.path).containsExactly(ReferenceDataDomain.Code.ABSENCE_TYPE of "PP")
      ma.occurrences.forEach { mo ->
        val occurrence = requireNotNull(findTemporaryAbsenceOccurrence(mo.id))
        val occRequest = authRequest.occurrences.single { it.legacyId == occurrence.legacyId }
        val omsa = requireNotNull(msaRepository.findByIdOrNull(occurrence.id))
        occurrence.verifyAgainst(occRequest, omsa)
        assertThat(occurrence.reasonPath.path).containsExactly(ReferenceDataDomain.Code.ABSENCE_TYPE of "PP")
        mo.movements.forEach { mm ->
          val movement = requireNotNull(findTemporaryAbsenceMovement(mm.id))
          val movementRequest = occRequest.movements.single { it.legacyId == movement.legacyId }
          val mmsa = requireNotNull(msaRepository.findByIdOrNull(movement.id))
          movement.verifyAgainst(auth.person.identifier, movementRequest, mmsa)
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

      val domainEvents = latestRevisionDomainEvents(auth)
      assertThat(domainEvents).hasSize(3)
      domainEvents.forEach { domainEvent -> assertThat(domainEvent.published).isTrue }
    }
  }

  @Test
  fun `200 ok - historic absences with movements get correct status`() {
    val prisonCode = prisonCode()
    val pi = personIdentifier()
    prisonerSearch.getPrisoners(prisonCode, setOf(pi))
    val request = migrateTapRequest(
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
    val response = migrateTap(pi, request).successResponse<MigrateTapResponse>()

    response.temporaryAbsences.first().also { ma ->
      ma.occurrences.first().also { mo ->
        val occurrence = requireNotNull(findTemporaryAbsenceOccurrence(mo.id))
        assertThat(occurrence.status.code).isEqualTo(TapOccurrenceStatus.Code.OVERDUE.name)
      }
    }

    response.temporaryAbsences.last().also { ma ->
      ma.occurrences.first().also { mo ->
        val occurrence = requireNotNull(findTemporaryAbsenceOccurrence(mo.id))
        assertThat(occurrence.status.code).isEqualTo(TapOccurrenceStatus.Code.EXPIRED.name)
      }
    }
  }

  private fun latestRevisionDomainEvents(entity: Identifiable): List<HmppsDomainEvent> = transactionTemplate.execute {
    val auditReader = AuditReaderFactory.get(entityManager)
    assertTrue(auditReader.isEntityClassAudited(entity::class.java))

    val revisionNumber =
      auditReader
        .getRevisions(entity::class.java, entity.id)
        .filterIsInstance<Long>()
        .max()

    auditReader
      .createQuery()
      .forRevisionsOfEntity(HmppsDomainEvent::class.java, true, true)
      .add(revisionNumber().eq(revisionNumber))
      .resultList
      .filterIsInstance<HmppsDomainEvent>()
  }!!

  private fun migrateTapRequest(
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
    created: AtAndBy = AtAndBy(LocalDateTime.now().minusHours(1), username()),
    updated: AtAndBy? = AtAndBy(LocalDateTime.now().minusHours(1), username()),
    legacyId: Long = newId(),
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
    comments,
    created,
    updated,
    legacyId,
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
    recordedBy: String = username(),
    recordedAt: LocalDateTime = LocalDateTime.now().minusDays(7),
  ) = TapMovement(
    occurredAt,
    direction,
    reasonCode,
    location,
    accompaniedByCode,
    accompaniedByComments,
    comments,
    AtAndByWithPrison(recordedAt, recordedBy, prisonCode),
    null,
    legacyId,
  )

  private fun migrateTap(
    personIdentifier: String,
    request: MigrateTapRequest,
    role: String? = Roles.NOMIS_SYNC,
  ) = webTestClient
    .put()
    .uri(MIGRATE_TAP_URL, personIdentifier)
    .bodyValue(request)
    .headers(setAuthorisation(username = SYSTEM_USERNAME, roles = listOfNotNull(role)))
    .exchange()

  companion object {
    const val MIGRATE_TAP_URL = "/migrate/temporary-absences/{personIdentifier}"

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
      assertThat(absenceReason?.code).isEqualTo(request.absenceReasonCode)
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
      assertThat(this.personIdentifier).isEqualTo(personIdentifier)
      assertThat(direction.name).isEqualTo(request.direction.name)
      assertThat(occurredAt).isCloseTo(request.occurredAt, within(2, SECONDS))
      assertThat(absenceReason.code).isEqualTo(request.absenceReasonCode)
      assertThat(accompaniedBy.code).isEqualTo(request.accompaniedByCode)
      assertThat(location).isEqualTo(request.location)
      assertThat(comments).isEqualTo(request.comments)
      assertThat(recordedByPrisonCode).isEqualTo(request.created.prisonCode)
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
