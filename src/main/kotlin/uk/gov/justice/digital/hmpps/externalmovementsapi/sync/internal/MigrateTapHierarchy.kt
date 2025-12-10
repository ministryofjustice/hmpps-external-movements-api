package uk.gov.justice.digital.hmpps.externalmovementsapi.sync.internal

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.DataSource
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.ExternalMovementContext
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.set
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.authorisation.TemporaryAbsenceAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.authorisation.TemporaryAbsenceAuthorisationRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.movement.TemporaryAbsenceMovement
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.movement.TemporaryAbsenceMovement.Direction.valueOf
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.movement.TemporaryAbsenceMovementRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.occurrence.TemporaryAbsenceOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.occurrence.TemporaryAbsenceOccurrenceRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.migration.MigrationSystemAudit
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.migration.MigrationSystemAuditRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.person.PersonSummary
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.AbsenceReason
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.AbsenceReasonCategory
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.AbsenceSubType
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.AbsenceType
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.AccompaniedBy
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.RdWithDomainLink
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceData
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_REASON
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_REASON_CATEGORY
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_SUB_TYPE
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_TYPE
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ACCOMPANIED_BY
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.TAP_AUTHORISATION_STATUS
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.TAP_OCCURRENCE_STATUS
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.TRANSPORT
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataPaths
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataRequired
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.TapAuthorisationStatus
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.TapOccurrenceStatus
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.Transport
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.of
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

@Transactional
@Service
class MigrateTapHierarchy(
  private val prisonerSearch: PrisonerSearchClient,
  private val referenceDataRepository: ReferenceDataRepository,
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

    val rdWithDomainLinks = referenceDataRepository.findMatchingWithDomainLink(request.requiredReferenceData())
    val findLinkedFrom = { id: Long -> referenceDataRepository.findLinkedFrom(id) }

    val person = personSummaryService.save(prisoner)
    val tap = request.temporaryAbsences.map { it.migrate(person, rdWithDomainLinks, findLinkedFrom) }
    val unscheduled = request.unscheduledMovements.map { it.migrate(person, null, rdWithDomainLinks, findLinkedFrom) }
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
    rdWithDomainLinks: List<RdWithDomainLink>,
    findLinked: (Long) -> List<ReferenceData>,
  ): MigratedAuthorisation {
    val rdPaths = rdPaths(rdWithDomainLinks, findLinked)
    val auth = authorisationRepository.save(asEntity(person, rdPaths))
    migrationSystemAuditRepository.save(MigrationSystemAudit(auth.id, created.at, created.by, updated?.at, updated?.by))
    val occurrences = occurrences.map { it.migrate(person, auth, rdWithDomainLinks, findLinked) }
    return MigratedAuthorisation(legacyId, auth.id, occurrences)
  }

  private fun TapOccurrence.migrate(
    person: PersonSummary,
    authorisation: TemporaryAbsenceAuthorisation,
    rdWithDomainLinks: List<RdWithDomainLink>,
    findLinked: (Long) -> List<ReferenceData>,
  ): MigratedOccurrence {
    val rdPaths = rdPaths(rdWithDomainLinks, findLinked)
    val occurrence = occurrenceRepository.save(
      asEntity(authorisation, rdPaths).calculateStatus {
        referenceDataRepository.findByKey(TAP_OCCURRENCE_STATUS of it) as TapOccurrenceStatus
      },
    )
    migrationSystemAuditRepository.save(MigrationSystemAudit(occurrence.id, created.at, created.by, updated?.at, updated?.by))
    val movements = movements.map { it.migrate(person, occurrence, rdWithDomainLinks, findLinked) }
    return MigratedOccurrence(legacyId, occurrence.id, movements)
  }

  private fun TapMovement.migrate(
    person: PersonSummary,
    occurrence: TemporaryAbsenceOccurrence?,
    rdWithDomainLinks: List<RdWithDomainLink>,
    findLinked: (Long) -> List<ReferenceData>,
  ): MigratedMovement {
    val rdPaths = rdPaths(rdWithDomainLinks, findLinked)
    val movement = movementRepository.save(asEntity(person.identifier, occurrence, rdPaths))
    migrationSystemAuditRepository.save(MigrationSystemAudit(movement.id, created.at, created.by, updated?.at, updated?.by))
    return MigratedMovement(legacyId, movement.id)
  }

  private fun TapAuthorisation.asEntity(
    person: PersonSummary,
    rdPaths: ReferenceDataPaths,
  ): TemporaryAbsenceAuthorisation {
    val reasonPath = rdPaths.reasonPath()
    val category = reasonPath.path.singleOrNull { it.domain == ABSENCE_REASON_CATEGORY }?.let {
      rdPaths.getReferenceData(it.domain, it.code)
    }
    return TemporaryAbsenceAuthorisation(
      person = person,
      prisonCode = prisonCode,
      absenceType = absenceTypeCode?.let { rdPaths.getReferenceData(ABSENCE_TYPE, it) as AbsenceType },
      absenceSubType = absenceSubTypeCode?.let {
        rdPaths.getReferenceData(ABSENCE_SUB_TYPE, it) as AbsenceSubType
      },
      absenceReasonCategory = category as? AbsenceReasonCategory,
      absenceReason = rdPaths.getReferenceData(ABSENCE_REASON, absenceReasonCode) as AbsenceReason,
      accompaniedBy = rdPaths.getReferenceData(ACCOMPANIED_BY, accompaniedByCode) as AccompaniedBy,
      transport = rdPaths.getReferenceData(TRANSPORT, transportCode) as Transport,
      repeat = repeat,
      status = rdPaths.getReferenceData(TAP_AUTHORISATION_STATUS, statusCode) as TapAuthorisationStatus,
      comments = notes,
      start = fromDate,
      end = toDate,
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
      rdPaths.getReferenceData(it.domain, it.code)
    }
    return TemporaryAbsenceOccurrence(
      authorisation = authorisation,
      absenceType = absenceTypeCode?.let {
        rdPaths.getReferenceData(
          ABSENCE_TYPE,
          it,
        ) as AbsenceType
      },
      absenceSubType = absenceSubTypeCode?.let {
        rdPaths.getReferenceData(ABSENCE_SUB_TYPE, it) as AbsenceSubType
      },
      absenceReasonCategory = category as? AbsenceReasonCategory,
      absenceReason = rdPaths.getReferenceData(ABSENCE_REASON, absenceReasonCode) as AbsenceReason,
      start = start,
      end = end,
      contactInformation = contactInformation,
      accompaniedBy = rdPaths.getReferenceData(ACCOMPANIED_BY, accompaniedByCode) as AccompaniedBy,
      transport = rdPaths.getReferenceData(TRANSPORT, transportCode) as Transport,
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
    rdPaths: ReferenceDataPaths,
  ) = TemporaryAbsenceMovement(
    personIdentifier = personIdentifier,
    occurrence = occurrence?.calculateStatus {
      referenceDataRepository.findByKey(TAP_OCCURRENCE_STATUS of it) as TapOccurrenceStatus
    },
    occurredAt = occurredAt,
    direction = valueOf(direction.name),
    absenceReason = rdPaths.getReferenceData(ABSENCE_REASON, absenceReasonCode) as AbsenceReason,
    accompaniedBy = rdPaths.getReferenceData(ACCOMPANIED_BY, accompaniedByCode) as AccompaniedBy,
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
    rdWithDomainLinks: List<RdWithDomainLink>,
    findLinked: (Long) -> List<ReferenceData>,
  ): ReferenceDataPaths = ReferenceDataPaths(rdWithDomainLinks.filter { it.referenceData.key in requiredReferenceData() }, findLinked)

  private fun List<MigratedAuthorisation>.scheduledEvents(personIdentifier: String): List<DomainEvent<*>> = flatMap { ma ->
    ma.occurrences.flatMap { mo ->
      mo.movements.movementEvents(personIdentifier) + mo.domainEvent(personIdentifier)
    } + ma.domainEvent(personIdentifier)
  }

  private fun List<MigratedMovement>.movementEvents(personIdentifier: String): List<DomainEvent<*>> = map {
    it.domainEvent(personIdentifier)
  }
}
