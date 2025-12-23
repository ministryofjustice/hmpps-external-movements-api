package uk.gov.justice.digital.hmpps.externalmovementsapi.sync.internal

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.DataSource
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.ExternalMovementContext
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.set
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.migration.MigrationSystemAudit
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.migration.MigrationSystemAuditRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.person.PersonSummary
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_REASON_CATEGORY
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataRequired
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.ReferenceDataPaths
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.authorisation.TemporaryAbsenceAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.authorisation.TemporaryAbsenceAuthorisationRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.movement.TemporaryAbsenceMovement
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.movement.TemporaryAbsenceMovement.Direction.valueOf
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.movement.TemporaryAbsenceMovementRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.occurrence.TemporaryAbsenceOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.occurrence.TemporaryAbsenceOccurrenceRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.AccompaniedBy
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.AuthorisationStatus
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.OccurrenceStatus
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.ReferenceData
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.ReferenceDataRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.Transport
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.absencereason.AbsenceCategorisationLinkRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.absencereason.AbsenceReason
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.absencereason.AbsenceReasonCategory
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.absencereason.AbsenceSubType
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.absencereason.AbsenceType
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.DomainEvent
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.HmppsDomainEvent
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.HmppsDomainEventRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.exception.NotFoundException
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.prisonersearch.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.occurrence.CancelOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.service.person.PersonSummaryService
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
class MigrateTapHierarchy(
  private val prisonerSearch: PrisonerSearchClient,
  private val referenceDataRepository: ReferenceDataRepository,
  private val linkRepository: AbsenceCategorisationLinkRepository,
  private val movementRepository: TemporaryAbsenceMovementRepository,
  private val occurrenceRepository: TemporaryAbsenceOccurrenceRepository,
  private val authorisationRepository: TemporaryAbsenceAuthorisationRepository,
  private val personSummaryService: PersonSummaryService,
  private val migrationSystemAuditRepository: MigrationSystemAuditRepository,
  private val domainEventRepository: HmppsDomainEventRepository,
) {
  fun migrate(personIdentifier: String, request: MigrateTapRequest): MigrateTapResponse {
    ExternalMovementContext.get().copy(source = DataSource.NOMIS, migratingData = true).set()
    val prisoner = prisonerSearch.getPrisoner(personIdentifier) ?: throw NotFoundException("Prisoner not found")
    removeTapForPersonIdentifier(personIdentifier)

    val allRd = referenceDataRepository.findAll()
    val rdLinks =
      linkRepository.findAll().groupBy({ it.id2 to it.domain1 }, { link -> allRd.first { it.id == link.id1 } })
    val findLinkedFrom: (UUID, ReferenceDataDomain.Code) -> List<ReferenceData> =
      { id: UUID, domainCode: ReferenceDataDomain.Code -> rdLinks[id to domainCode] ?: emptyList() }

    val person = personSummaryService.save(prisoner)
    val tap = request.temporaryAbsences.map { it.migrate(person, allRd, findLinkedFrom) }
    val unscheduled = request.unscheduledMovements.map { it.migrate(person, null, allRd) }
    persistMigrationEvents(personIdentifier, tap, unscheduled)
    return MigrateTapResponse(tap, unscheduled)
  }

  private fun removeTapForPersonIdentifier(personIdentifier: String) {
    movementRepository.deleteByPersonIdentifier(personIdentifier)
    occurrenceRepository.deleteByAuthorisationPersonIdentifier(personIdentifier)
    authorisationRepository.deleteByPersonIdentifier(personIdentifier)
  }

  private fun TapAuthorisation.migrate(
    person: PersonSummary,
    rd: List<ReferenceData>,
    findLinked: (UUID, ReferenceDataDomain.Code) -> List<ReferenceData>,
  ): MigratedAuthorisation {
    val rdPaths = rdPaths(rd, findLinked)
    val auth = authorisationRepository.save(asEntity(person, rdPaths))
    migrationSystemAuditRepository.save(MigrationSystemAudit(auth.id, created.at, created.by, updated?.at, updated?.by))
    val occurrences = occurrences.map { it.migrate(person, auth, rd, findLinked) }
    return MigratedAuthorisation(legacyId, auth.id, occurrences)
  }

  private fun TapOccurrence.migrate(
    person: PersonSummary,
    authorisation: TemporaryAbsenceAuthorisation,
    rd: List<ReferenceData>,
    findLinked: (UUID, ReferenceDataDomain.Code) -> List<ReferenceData>,
  ): MigratedOccurrence {
    val rdPaths = rdPaths(rd, findLinked)
    val occ = asEntity(authorisation, rdPaths)
    val movements = movements.map { it.migrate(person, occ, rd) }
    val occurrence = occurrenceRepository.save(
      occ.calculateStatus {
        rdPaths.getReferenceData(OccurrenceStatus::class, it) as OccurrenceStatus
      },
    )
    migrationSystemAuditRepository.save(
      MigrationSystemAudit(
        occurrence.id,
        created.at,
        created.by,
        updated?.at,
        updated?.by,
      ),
    )
    return MigratedOccurrence(legacyId, occurrence.id, movements)
  }

  private fun TapMovement.migrate(
    person: PersonSummary,
    occurrence: TemporaryAbsenceOccurrence?,
    rd: List<ReferenceData>,
  ): MigratedMovement {
    val rdSupplier =
      { domain: KClass<out ReferenceData>, code: String -> rd.first { domain.isInstance(it) && it.code == code } }
    val movement = asEntity(person.identifier, occurrence, rdSupplier)
    occurrence?.also { occ ->
      occ.addMovement(movement) {
        rdSupplier(OccurrenceStatus::class, it) as OccurrenceStatus
      }
    }
    if (occurrence == null) {
      movementRepository.save(movement)
    }
    migrationSystemAuditRepository.save(
      MigrationSystemAudit(
        movement.id,
        created.at,
        created.by,
        updated?.at,
        updated?.by,
      ),
    )
    return MigratedMovement(legacyId, movement.id)
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
      absenceSubType = absenceSubTypeCode?.let {
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
      schedule = null,
      reasonPath = reasonPath,
      legacyId = legacyId,
    )
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
      absenceSubType = absenceSubTypeCode?.let {
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

  private fun TapMovement.asEntity(
    personIdentifier: String,
    occurrence: TemporaryAbsenceOccurrence?,
    rdSupplier: (KClass<out ReferenceData>, String) -> ReferenceData,
  ) = TemporaryAbsenceMovement(
    personIdentifier = personIdentifier,
    occurrence = occurrence?.calculateStatus {
      rdSupplier(OccurrenceStatus::class, it) as OccurrenceStatus
    },
    occurredAt = occurredAt,
    direction = valueOf(direction.name),
    absenceReason = rdSupplier(AbsenceReason::class, absenceReasonCode) as AbsenceReason,
    accompaniedBy = rdSupplier(AccompaniedBy::class, accompaniedByCode) as AccompaniedBy,
    accompaniedByComments = accompaniedByComments,
    comments = comments,
    recordedByPrisonCode = created.prisonCode,
    location = location,
    legacyId = legacyId,
  )

  private fun persistMigrationEvents(
    personIdentifier: String,
    tap: List<MigratedAuthorisation>,
    unscheduled: List<MigratedMovement>,
  ) {
    val domainEvents = tap.scheduledEvents(personIdentifier) + unscheduled.movementEvents(personIdentifier)
    domainEventRepository.saveAll(domainEvents.map { HmppsDomainEvent(it).apply { published = true } })
  }

  private fun ReferenceDataRequired.rdPaths(
    rd: List<ReferenceData>,
    findLinked: (UUID, ReferenceDataDomain.Code) -> List<ReferenceData>,
  ): ReferenceDataPaths = ReferenceDataPaths(
    rd.filter { rd -> rd::class to rd.code in requiredReferenceData().map { it.domain.clazz to it.code } },
    findLinked,
  )

  private fun List<MigratedAuthorisation>.scheduledEvents(personIdentifier: String): List<DomainEvent<*>> = flatMap { ma ->
    ma.occurrences.flatMap { mo ->
      mo.movements.movementEvents(personIdentifier) + mo.domainEvent(personIdentifier)
    } + ma.domainEvent(personIdentifier)
  }

  private fun List<MigratedMovement>.movementEvents(personIdentifier: String): List<DomainEvent<*>> = map {
    it.domainEvent(personIdentifier)
  }
}
