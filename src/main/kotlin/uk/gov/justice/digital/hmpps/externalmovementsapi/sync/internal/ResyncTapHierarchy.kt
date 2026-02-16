package uk.gov.justice.digital.hmpps.externalmovementsapi.sync.internal

import com.fasterxml.jackson.databind.ObjectMapper
import com.microsoft.applicationinsights.TelemetryClient
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.DataSource
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.ExternalMovementContext
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.ExternalMovementContext.Companion.SYSTEM_USERNAME
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.set
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.migration.MigrationSystemAudit
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.migration.MigrationSystemAuditRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.person.PersonSummary
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceData
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_REASON_CATEGORY
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataRequired
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.ReferenceDataPaths
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.authorisation.TemporaryAbsenceAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.authorisation.TemporaryAbsenceAuthorisationRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.authorisation.authorisationMatchesPersonIdentifier
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.movement.TemporaryAbsenceMovement
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.movement.TemporaryAbsenceMovement.Direction.valueOf
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.movement.TemporaryAbsenceMovementRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.occurrence.TemporaryAbsenceOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.occurrence.TemporaryAbsenceOccurrenceRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.occurrence.occurrenceMatchesPersonIdentifier
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.AccompaniedBy
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.AuthorisationStatus
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.OccurrenceStatus
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.Transport
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.absencereason.AbsenceCategorisationLinkRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.absencereason.AbsenceReason
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.absencereason.AbsenceReasonCategory
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.absencereason.AbsenceSubType
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.absencereason.AbsenceType
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.authorisation.ChangeAuthorisationComments
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.authorisation.ChangeAuthorisationLocations
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.authorisation.ChangePrisonPerson
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.movement.ChangeMovementAccompaniment
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.movement.ChangeMovementComments
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.movement.ChangeMovementDirection
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.movement.ChangeMovementLocation
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.movement.ChangeMovementOccurredAt
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.movement.ChangeMovementOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.movement.ChangeMovementReason
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.occurrence.CancelOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.occurrence.ChangeOccurrenceComments
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.occurrence.ChangeOccurrenceLocation
import uk.gov.justice.digital.hmpps.externalmovementsapi.service.person.PersonSummaryService
import uk.gov.justice.digital.hmpps.externalmovementsapi.sync.AtAndBy
import uk.gov.justice.digital.hmpps.externalmovementsapi.sync.migrate.MigrateTapRequest
import uk.gov.justice.digital.hmpps.externalmovementsapi.sync.migrate.MigrateTapResponse
import uk.gov.justice.digital.hmpps.externalmovementsapi.sync.migrate.MigratedAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.sync.migrate.MigratedMovement
import uk.gov.justice.digital.hmpps.externalmovementsapi.sync.migrate.MigratedOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.sync.migrate.TapAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.sync.migrate.TapMovement
import uk.gov.justice.digital.hmpps.externalmovementsapi.sync.migrate.TapOccurrence
import java.time.LocalDate
import java.util.UUID
import kotlin.reflect.KClass

@Transactional
@Service
class ResyncTapHierarchy(
  private val referenceDataRepository: ReferenceDataRepository,
  private val linkRepository: AbsenceCategorisationLinkRepository,
  private val movementRepository: TemporaryAbsenceMovementRepository,
  private val occurrenceRepository: TemporaryAbsenceOccurrenceRepository,
  private val authorisationRepository: TemporaryAbsenceAuthorisationRepository,
  private val personSummaryService: PersonSummaryService,
  private val migrationSystemAuditRepository: MigrationSystemAuditRepository,
  private val objectMapper: ObjectMapper,
  private val telemetryClient: TelemetryClient,
) {
  fun resync(personIdentifier: String, request: MigrateTapRequest): MigrateTapResponse {
    ExternalMovementContext.get().copy(username = SYSTEM_USERNAME, source = DataSource.NOMIS, migratingData = false)
      .set()
    val person = personSummaryService.getWithSave(personIdentifier)
    val allRd = referenceDataRepository.findAll()
    val rdLinks =
      linkRepository.findAll().groupBy({ it.id2 to it.domain1 }, { link -> allRd.first { it.id == link.id1 } })
    val findLinkedFrom: (UUID, ReferenceDataDomain.Code) -> List<ReferenceData> =
      { id: UUID, domainCode: ReferenceDataDomain.Code -> rdLinks[id to domainCode] ?: emptyList() }

    val authorisations: List<TemporaryAbsenceAuthorisation> =
      authorisationRepository.findAll(authorisationMatchesPersonIdentifier(personIdentifier))
    val authorisationProvider = { id: UUID?, legacyId: Long ->
      authorisations.firstOrNull { it.id == id || it.legacyId == legacyId }
    }
    val occurrences: List<TemporaryAbsenceOccurrence> =
      occurrenceRepository.findAll(occurrenceMatchesPersonIdentifier(personIdentifier))
    val occurrenceProvider = { id: UUID?, legacyId: Long ->
      occurrences.firstOrNull { it.id == id || it.legacyId == legacyId }
    }
    val movements: List<TemporaryAbsenceMovement> = movementRepository.findAllByPersonIdentifier(personIdentifier)
    val movementProvider = { id: UUID?, legacyId: String ->
      movements.firstOrNull { it.id == id || it.legacyId == legacyId }
    }

    val tap = request.temporaryAbsences.map {
      it.resync(person, allRd, findLinkedFrom, authorisationProvider, occurrenceProvider, movementProvider)
    }
    val unscheduled = request.unscheduledMovements.map { it.resync(person, null, allRd, movementProvider) }
    removeNotInResync(tap, unscheduled, authorisations, occurrences, movements)
    return MigrateTapResponse(tap, unscheduled)
  }

  private fun TapAuthorisation.resync(
    person: PersonSummary,
    rd: List<ReferenceData>,
    findLinked: (UUID, ReferenceDataDomain.Code) -> List<ReferenceData>,
    authorisationProvider: (UUID?, Long) -> TemporaryAbsenceAuthorisation?,
    occurrenceProvider: (UUID?, Long) -> TemporaryAbsenceOccurrence?,
    movementProvider: (UUID?, String) -> TemporaryAbsenceMovement?,
  ): MigratedAuthorisation {
    val rdPaths = rdPaths(rd, findLinked)
    val auth = (
      authorisationProvider(id, legacyId)
        ?: id?.let { authorisationRepository.findByIdOrNull(it) }
        ?: authorisationRepository.findByLegacyId(legacyId)
      )
      ?.update(person, this, rdPaths)
      ?: authorisationRepository.save(asEntity(person, rdPaths))
    val occurrences = occurrences.map {
      it.resync(person, auth, rd, findLinked, occurrenceProvider, movementProvider)
    }
    mergeMigrationAudit(auth.id, created, updated)
    return MigratedAuthorisation(legacyId, auth.id, occurrences)
  }

  private fun TapOccurrence.resync(
    person: PersonSummary,
    authorisation: TemporaryAbsenceAuthorisation,
    rd: List<ReferenceData>,
    findLinked: (UUID, ReferenceDataDomain.Code) -> List<ReferenceData>,
    occurrenceProvider: (UUID?, Long) -> TemporaryAbsenceOccurrence?,
    movementProvider: (UUID?, String) -> TemporaryAbsenceMovement?,
  ): MigratedOccurrence {
    val rdPaths = rdPaths(rd, findLinked)
    val occ = (
      occurrenceProvider(id, legacyId)
        ?: id?.let { occurrenceRepository.findByIdOrNull(it) }
        ?: occurrenceRepository.findByLegacyId(legacyId)
      )
      ?.update(authorisation, this, rdPaths)
      ?: asEntity(authorisation, rdPaths)
    val movements = movements.map { it.resync(person, occ, rd, movementProvider) }
    if (movements.map { it.id }.sorted() != occ.movements().map { it.id }.sorted()) {
      val toRemove = occ.movements().filter { m -> m.id !in movements.map { it.id } }
      toRemove.forEach { mov ->
        occ.removeMovement(mov) { rdPaths.getReferenceData(OccurrenceStatus::class, it) as OccurrenceStatus }
      }
    }
    val occurrence = occurrenceRepository.save(
      occ.calculateStatus {
        rdPaths.getReferenceData(OccurrenceStatus::class, it) as OccurrenceStatus
      },
    )
    mergeMigrationAudit(occurrence.id, created, updated)
    return MigratedOccurrence(legacyId, occurrence.id, movements)
  }

  private fun TapMovement.resync(
    person: PersonSummary,
    occurrence: TemporaryAbsenceOccurrence?,
    rd: List<ReferenceData>,
    movementProvider: (UUID?, String) -> TemporaryAbsenceMovement?,
  ): MigratedMovement {
    val rdSupplier =
      { domain: KClass<out ReferenceData>, code: String -> rd.first { domain.isInstance(it) && it.code == code } }
    val movement = (
      movementProvider(id, legacyId)
        ?: id?.let { movementRepository.findByIdOrNull(it) }
        ?: movementRepository.findByLegacyId(legacyId)
      )
      ?.update(person, occurrence, this, rdSupplier)
      ?: asEntity(person, rdSupplier)
    occurrence?.also { occ ->
      if (occurrence.movements().none { it.id == id }) {
        occ.addMovement(movement) {
          ExternalMovementContext.get().copy(reason = null).set()
          rdSupplier(OccurrenceStatus::class, it) as OccurrenceStatus
        }
      }
    } ?: movementRepository.save(movement)
    mergeMigrationAudit(movement.id, created, updated)
    return MigratedMovement(legacyId, movement.id)
  }

  private fun removeNotInResync(
    tap: List<MigratedAuthorisation>,
    unscheduled: List<MigratedMovement>,
    authorisations: List<TemporaryAbsenceAuthorisation>,
    occurrences: List<TemporaryAbsenceOccurrence>,
    movements: List<TemporaryAbsenceMovement>,
  ) {
    val movementIds =
      tap.flatMap { a -> a.occurrences.flatMap { o -> o.movements.map { m -> m.id } } } + unscheduled.map { it.id }
    val occurrenceIds = tap.flatMap { a -> a.occurrences.map { o -> o.id } }
    val authorisationIds = tap.map { a -> a.id }.toSet()
    movements.filter { it.id !in movementIds }.also(movementRepository::deleteAll)
    occurrences.filter { it.id !in occurrenceIds }.also(occurrenceRepository::deleteAll)
    authorisations.filter { it.id !in authorisationIds }.also(authorisationRepository::deleteAll)
  }

  private fun TapAuthorisation.asEntity(
    person: PersonSummary,
    rdPaths: ReferenceDataPaths,
  ): TemporaryAbsenceAuthorisation {
    val reasonPath = rdPaths.reasonPath()
    val category = reasonPath.path.singleOrNull { it.domain == ABSENCE_REASON_CATEGORY }?.let {
      rdPaths.findReferenceData(it.domain.clazz, it.code) as? AbsenceReasonCategory
    }
    val status = rdPaths.getReferenceData(AuthorisationStatus::class, statusCode) as AuthorisationStatus
    return TemporaryAbsenceAuthorisation(
      person = person,
      prisonCode = prisonCode,
      absenceType = absenceTypeCode?.let { rdPaths.findReferenceData(AbsenceType::class, it) as? AbsenceType },
      absenceSubType = absenceSubTypeCode?.takeIf { it != AbsenceSubType.Code.SECURITY_ESCORT.value }?.let {
        rdPaths.findReferenceData(AbsenceSubType::class, it) as? AbsenceSubType
      },
      absenceReasonCategory = category,
      absenceReason = rdPaths.getReferenceData(AbsenceReason::class, absenceReasonCode) as AbsenceReason,
      accompaniedBy = rdPaths.getReferenceData(AccompaniedBy::class, accompaniedByCode) as AccompaniedBy,
      transport = rdPaths.getReferenceData(Transport::class, transportCode) as Transport,
      repeat = repeat,
      status = if (status.code == AuthorisationStatus.Code.PENDING.name && end.isBefore(LocalDate.now())) {
        rdPaths.getReferenceData(
          AuthorisationStatus::class,
          AuthorisationStatus.Code.EXPIRED.name,
        ) as AuthorisationStatus
      } else {
        status
      },
      comments = comments,
      start = start,
      end = end,
      schedule = schedule()?.let { objectMapper.valueToTree(it) },
      reasonPath = reasonPath,
      locations = occurrences.mapTo(linkedSetOf()) { it.location }.takeIf { it.isNotEmpty() }
        ?: location?.let { linkedSetOf(it) } ?: linkedSetOf(),
      legacyId = legacyId,
    )
  }

  private fun TemporaryAbsenceAuthorisation.update(
    person: PersonSummary,
    request: TapAuthorisation,
    rdPaths: ReferenceDataPaths,
  ) = apply {
    applyPrisonPerson(ChangePrisonPerson(person.identifier, request.prisonCode)) { person }
    applyAbsenceCategorisation(request, rdPaths)
    checkStatus(request, rdPaths)
    checkSchedule(request, rdPaths)
    applyLogistics(request, rdPaths)
    applyComments(ChangeAuthorisationComments(request.comments))
    val occurrences = occurrenceRepository.findByAuthorisationId(id)
    if (occurrences.isEmpty()) {
      request.schedule()?.also { applySchedule(objectMapper.valueToTree(it)) }
    }
    val locations = occurrences.mapTo(linkedSetOf()) { it.location }.takeIf { it.isNotEmpty() }
      ?: request.location?.let { linkedSetOf(it) }
    locations?.also { applyLocations(ChangeAuthorisationLocations(it)) }
  }

  private fun TapOccurrence.asEntity(
    authorisation: TemporaryAbsenceAuthorisation,
    rdPaths: ReferenceDataPaths,
  ): TemporaryAbsenceOccurrence {
    val reasonPath = rdPaths.reasonPath()
    val category = reasonPath.path.singleOrNull { it.domain == ABSENCE_REASON_CATEGORY }?.let {
      rdPaths.findReferenceData(it.domain.clazz, it.code) as? AbsenceReasonCategory
    }
    return TemporaryAbsenceOccurrence(
      authorisation = authorisation,
      absenceType = absenceTypeCode?.let {
        rdPaths.findReferenceData(AbsenceType::class, it) as? AbsenceType
      },
      absenceSubType = absenceSubTypeCode?.takeIf { it != AbsenceSubType.Code.SECURITY_ESCORT.value }?.let {
        rdPaths.findReferenceData(AbsenceSubType::class, it) as? AbsenceSubType
      },
      absenceReasonCategory = category,
      absenceReason = rdPaths.getReferenceData(AbsenceReason::class, absenceReasonCode) as AbsenceReason,
      start = start,
      end = end,
      contactInformation = contactInformation,
      accompaniedBy = rdPaths.getReferenceData(AccompaniedBy::class, accompaniedByCode) as AccompaniedBy,
      transport = rdPaths.getReferenceData(Transport::class, transportCode) as Transport,
      location = location,
      comments = comments,
      legacyId = legacyId,
      reasonPath = reasonPath,
      scheduleReference = null,
    ).apply {
      if (isCancelled) {
        cancel(CancelOccurrence(), rdPaths::getReferenceData)
      }
    }
  }

  private fun TemporaryAbsenceOccurrence.update(
    authorisation: TemporaryAbsenceAuthorisation,
    request: TapOccurrence,
    rdPaths: ReferenceDataPaths,
  ) = apply {
    if (this.authorisation.id != authorisation.id) {
      telemetryClient.trackEvent(
        "OccurrenceAuthorisationSwitch",
        mapOf(
          "occurrence" to id.toString(),
          "fromAuth" to this.authorisation.id.toString(),
          "toAuth" to authorisation.id.toString(),
          "legacyId" to legacyId.toString(),
        ),
        mapOf(),
      )
    }
    authorisationPersonAndPrison(authorisation)
    applyAbsenceCategorisation(request, rdPaths)
    applySchedule(request)
    applyLogistics(request, rdPaths)
    checkCancellation(request, rdPaths)
    applyComments(ChangeOccurrenceComments(request.comments))
    if (request.isCancelled && request.movements.isEmpty()) {
      cancel(CancelOccurrence(), rdPaths::getReferenceData)
    }
  }

  private fun TapMovement.asEntity(
    person: PersonSummary,
    rdSupplier: (KClass<out ReferenceData>, String) -> ReferenceData,
  ) = TemporaryAbsenceMovement(
    person = person,
    occurrence = null,
    occurredAt = occurredAt,
    direction = valueOf(direction.name),
    absenceReason = rdSupplier(AbsenceReason::class, absenceReasonCode) as AbsenceReason,
    accompaniedBy = rdSupplier(AccompaniedBy::class, accompaniedByCode) as AccompaniedBy,
    accompaniedByComments = accompaniedByComments,
    comments = comments,
    prisonCode = prisonCode,
    location = location,
    legacyId = legacyId,
  )

  private fun TemporaryAbsenceMovement.update(
    person: PersonSummary,
    occurrence: TemporaryAbsenceOccurrence?,
    request: TapMovement,
    rdProvider: (KClass<out ReferenceData>, String) -> ReferenceData,
  ) = apply {
    moveTo(person)
    switchSchedule(ChangeMovementOccurrence(occurrence?.id), rdProvider) { _ -> checkNotNull(occurrence) }
    applyDirection(ChangeMovementDirection(request.direction))
    applyOccurredAt(ChangeMovementOccurredAt(request.occurredAt))
    applyComments(ChangeMovementComments(request.comments))
    applyAccompaniedBy(
      ChangeMovementAccompaniment(
        request.accompaniedByCode,
        request.accompaniedByComments,
      ),
      rdProvider,
    )
    applyReason(ChangeMovementReason(request.absenceReasonCode), rdProvider)
    if (occurrence != null) {
      occurrence.applyLocation(ChangeOccurrenceLocation(request.location))
    } else {
      applyLocation(ChangeMovementLocation(request.location))
    }
  }

  private fun ReferenceDataRequired.rdPaths(
    rd: List<ReferenceData>,
    findLinked: (UUID, ReferenceDataDomain.Code) -> List<ReferenceData>,
  ): ReferenceDataPaths = ReferenceDataPaths(
    rd.filter { rd -> rd::class to rd.code in requiredReferenceData().map { it.domain.clazz to it.code } },
    findLinked,
  )

  private fun mergeMigrationAudit(id: UUID, created: AtAndBy, updated: AtAndBy?) {
    migrationSystemAuditRepository.save(
      MigrationSystemAudit(id, created.at, created.by, updated?.at, updated?.by),
    )
  }
}
