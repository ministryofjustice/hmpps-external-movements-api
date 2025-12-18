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
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_REASON
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_REASON_CATEGORY
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_SUB_TYPE
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_TYPE
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ACCOMPANIED_BY
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.TAP_AUTHORISATION_STATUS
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.TAP_OCCURRENCE_STATUS
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.TRANSPORT
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataLinkRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataPaths
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataRequired
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.TapAuthorisationStatus
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.TapAuthorisationStatus.Code.EXPIRED
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.TapAuthorisationStatus.Code.PENDING
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.TapOccurrenceStatus
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.Transport
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

@Transactional
@Service
class MigrateTapHierarchy(
  private val prisonerSearch: PrisonerSearchClient,
  private val referenceDataRepository: ReferenceDataRepository,
  private val referenceDataLinkRepository: ReferenceDataLinkRepository,
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
    val rdLinks = referenceDataLinkRepository.findAll().groupBy({ it.rd2.id to it.rd1.domain }, { it.rd1 })
    val findLinkedFrom: (Long, ReferenceDataDomain.Code) -> List<ReferenceData> =
      { id: Long, domainCode: ReferenceDataDomain.Code -> (rdLinks[id to domainCode]) ?: emptyList() }

    val person = personSummaryService.save(prisoner)
    val tap = request.temporaryAbsences.map { it.migrate(person, rdWithDomainLinks, findLinkedFrom) }
    val unscheduled = request.unscheduledMovements.map { it.migrate(person, null, rdWithDomainLinks) }
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
    findLinked: (Long, ReferenceDataDomain.Code) -> List<ReferenceData>,
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
    findLinked: (Long, ReferenceDataDomain.Code) -> List<ReferenceData>,
  ): MigratedOccurrence {
    val rdPaths = rdPaths(rdWithDomainLinks, findLinked)
    val occ = asEntity(authorisation, rdPaths)
    val movements = movements.map { it.migrate(person, occ, rdWithDomainLinks) }
    val occurrence = occurrenceRepository.save(
      occ.calculateStatus {
        rdPaths.getReferenceData(TAP_OCCURRENCE_STATUS, it) as TapOccurrenceStatus
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
    rdWithDomainLinks: List<RdWithDomainLink>,
  ): MigratedMovement {
    val rdSupplier =
      { domain: ReferenceDataDomain.Code, code: String -> rdWithDomainLinks.first { it.referenceData.domain == domain && it.referenceData.code == code }.referenceData }
    val movement = asEntity(person.identifier, occurrence, rdSupplier)
    occurrence?.also { occ ->
      occ.addMovement(movement) {
        rdSupplier(TAP_OCCURRENCE_STATUS, it) as TapOccurrenceStatus
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
      rdPaths.findReferenceData(it.domain, it.code) as? AbsenceReasonCategory
    }
    val status = rdPaths.getReferenceData(TAP_AUTHORISATION_STATUS, statusCode) as TapAuthorisationStatus
    return TemporaryAbsenceAuthorisation(
      person = person,
      prisonCode = prisonCode,
      absenceType = absenceTypeCode?.let { rdPaths.findReferenceData(ABSENCE_TYPE, it) as? AbsenceType },
      absenceSubType = absenceSubTypeCode?.let {
        rdPaths.findReferenceData(ABSENCE_SUB_TYPE, it) as? AbsenceSubType
      },
      absenceReasonCategory = category,
      absenceReason = rdPaths.getReferenceData(ABSENCE_REASON, absenceReasonCode) as AbsenceReason,
      accompaniedBy = rdPaths.getReferenceData(ACCOMPANIED_BY, accompaniedByCode) as AccompaniedBy,
      transport = rdPaths.getReferenceData(TRANSPORT, transportCode) as Transport,
      repeat = repeat,
      status = if (status.code == PENDING.name && end.isBefore(LocalDate.now())) {
        rdPaths.getReferenceData(TAP_AUTHORISATION_STATUS, EXPIRED.name) as TapAuthorisationStatus
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
      rdPaths.findReferenceData(it.domain, it.code) as? AbsenceReasonCategory
    }
    return TemporaryAbsenceOccurrence(
      authorisation = authorisation,
      absenceType = absenceTypeCode?.let {
        rdPaths.findReferenceData(ABSENCE_TYPE, it) as? AbsenceType
      },
      absenceSubType = absenceSubTypeCode?.let {
        rdPaths.findReferenceData(ABSENCE_SUB_TYPE, it) as? AbsenceSubType
      },
      absenceReasonCategory = category,
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
    rdSupplier: (ReferenceDataDomain.Code, String) -> ReferenceData,
  ) = TemporaryAbsenceMovement(
    personIdentifier = personIdentifier,
    occurrence = occurrence?.calculateStatus {
      rdSupplier(TAP_OCCURRENCE_STATUS, it) as TapOccurrenceStatus
    },
    occurredAt = occurredAt,
    direction = valueOf(direction.name),
    absenceReason = rdSupplier(ABSENCE_REASON, absenceReasonCode) as AbsenceReason,
    accompaniedBy = rdSupplier(ACCOMPANIED_BY, accompaniedByCode) as AccompaniedBy,
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
    findLinked: (Long, ReferenceDataDomain.Code) -> List<ReferenceData>,
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
