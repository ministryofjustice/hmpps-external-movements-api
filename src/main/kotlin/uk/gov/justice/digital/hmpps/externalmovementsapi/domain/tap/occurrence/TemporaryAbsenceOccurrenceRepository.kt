package uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.occurrence

import jakarta.persistence.QueryHint
import jakarta.persistence.criteria.JoinType
import org.hibernate.jpa.HibernateHints
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.jpa.repository.QueryHints
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.RefreshRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.person.PersonSummary
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.person.matchesIdentifier
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.person.matchesName
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceData
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceData.Companion.CODE
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.authorisation.TemporaryAbsenceAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.occurrence.TemporaryAbsenceOccurrence.Companion.AUTHORISATION
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.occurrence.TemporaryAbsenceOccurrence.Companion.END
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.occurrence.TemporaryAbsenceOccurrence.Companion.PERSON
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.occurrence.TemporaryAbsenceOccurrence.Companion.PRISON_CODE
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.occurrence.TemporaryAbsenceOccurrence.Companion.START
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.occurrence.TemporaryAbsenceOccurrence.Companion.STATUS
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.OccurrenceStatus
import uk.gov.justice.digital.hmpps.externalmovementsapi.exception.NotFoundException
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.DateRange
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.paged.AbsenceCategorisationFilter
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.SequencedSet
import java.util.UUID

interface TemporaryAbsenceOccurrenceRepository :
  JpaRepository<TemporaryAbsenceOccurrence, UUID>,
  JpaSpecificationExecutor<TemporaryAbsenceOccurrence>,
  RefreshRepository<TemporaryAbsenceAuthorisation, UUID> {
  @Query(
    """
    select 
        min(cast(tao.start as LocalDate)) as start, 
        max(cast(tao.end as LocalDate)) as end 
    from TemporaryAbsenceOccurrence tao where tao.authorisation.id = :authorisationId
    group by tao.authorisation.id
    """,
  )
  fun dateRangeForAuthorisation(authorisationId: UUID): DateRange?

  fun findByLegacyId(legacyId: Long): TemporaryAbsenceOccurrence?

  fun countByAuthorisationId(authorisationId: UUID): Int
  fun findByAuthorisationId(authorisationId: UUID): List<TemporaryAbsenceOccurrence>
  fun findByAuthorisationIdIn(authorisationIds: Set<UUID>): List<TemporaryAbsenceOccurrence>

  @Query(
    """
     with occurrences as (
      select ao.id as occurrenceId, row_number() over (partition by ao.authorisation.id order by ao.start, ao.end) as pos
      from TemporaryAbsenceOccurrence ao 
      where ao.authorisation.id = :authorisationId
    )
    select o.pos as position, (select count(1) from occurrences) as total
    from occurrences o where o.occurrenceId = :occurrenceId
    """,
  )
  fun getPosition(authorisationId: UUID, occurrenceId: UUID): TapOccurrencePosition

  @Query(
    """
      select tao from TemporaryAbsenceOccurrence tao
      where tao.end < :dateTime
        and tao.status.code in :statuses
    """,
  )
  @QueryHints(value = [QueryHint(name = HibernateHints.HINT_NATIVE_LOCK_MODE, value = "UPGRADE-SKIPLOCKED")])
  fun findPastOccurrences(
    statuses: Set<String>,
    dateTime: LocalDateTime,
    pageable: Pageable,
  ): List<TemporaryAbsenceOccurrence>

  @Query(
    """
    select count(1) as leavingToday
    from tap.occurrence tao
        join tap.authorisation taa on taa.id = tao.authorisation_id
        join tap.authorisation_status ast on ast.id = taa.status_id
        join tap.occurrence_status ost on ost.id = tao.status_id
    where tao.prison_code = :prisonIdentifier
      and ast.code = 'APPROVED'
      and ost.code <> 'CANCELLED'
      and tao.start between current_date and (current_date + 1)
  """,
    nativeQuery = true,
  )
  fun findLeavingTodayCount(prisonIdentifier: String): Int

  @Query(
    """
    select count(1) as returningToday
    from tap.occurrence tao
        join tap.authorisation taa on taa.id = tao.authorisation_id
        join tap.authorisation_status ast on ast.id = taa.status_id
        join tap.occurrence_status ost on ost.id = tao.status_id
    where tao.prison_code = :prisonIdentifier
      and ast.code = 'APPROVED'
      and ost.code <> 'CANCELLED'
      and tao.end between current_date and (current_date + 1)
  """,
    nativeQuery = true,
  )
  fun findReturningTodayCount(prisonIdentifier: String): Int

  fun countByPersonIdentifier(personIdentifier: String): Int
  fun findByPersonIdentifier(personIdentifier: String): List<TemporaryAbsenceOccurrence>

  @Modifying
  @Query(
    """
    delete from TemporaryAbsenceOccurrence tao where tao.person.identifier = :personIdentifier
  """,
  )
  fun deleteByPersonIdentifier(personIdentifier: String)

  @Query("""select tao.id from TemporaryAbsenceOccurrence tao where tao.person.identifier = :personIdentifier""")
  fun findIdsByPersonIdentifier(personIdentifier: String): List<UUID>

  @Query("""select tao.id from TemporaryAbsenceOccurrence tao where tao.legacyId in :legacyIds""")
  fun findIdsByLegacyId(legacyIds: Set<Long>): List<UUID>

  @EntityGraph("tap.occurrence.full")
  override fun findAllById(ids: Iterable<UUID>): List<TemporaryAbsenceOccurrence>
}

fun TemporaryAbsenceOccurrenceRepository.getOccurrence(id: UUID) = findByIdOrNull(id) ?: throw NotFoundException("Temporary absence occurrence not found")

fun occurrenceMatchesPrisonCode(prisonCode: String) = Specification<TemporaryAbsenceOccurrence> { tao, _, cb ->
  cb.equal(tao.get<String>(PRISON_CODE), prisonCode)
}

fun occurrenceMatchesPersonIdentifier(personIdentifier: String) = Specification<TemporaryAbsenceOccurrence> { tao, _, cb ->
  tao.join<TemporaryAbsenceOccurrence, PersonSummary>(PERSON, JoinType.INNER).matchesIdentifier(cb, personIdentifier)
}

fun occurrencePersonIdentifierIn(personIdentifiers: SequencedSet<String>) = Specification<TemporaryAbsenceOccurrence> { tao, _, _ ->
  val person = tao.join<TemporaryAbsenceOccurrence, PersonSummary>(PERSON, JoinType.INNER)
  person.get<String>(PersonSummary.IDENTIFIER).`in`(personIdentifiers)
}

fun occurrenceMatchesPersonName(name: String) = Specification<TemporaryAbsenceOccurrence> { tao, _, cb ->
  tao.join<TemporaryAbsenceOccurrence, PersonSummary>(PERSON, JoinType.INNER).matchesName(cb, name)
}

fun occurrenceOverlapsDateRange(start: LocalDate?, end: LocalDate?) = Specification<TemporaryAbsenceOccurrence> { tao, _, cb ->
  cb.and(
    start?.let { cb.greaterThanOrEqualTo(tao.get(END), it) } ?: cb.conjunction(),
    end?.let { cb.lessThanOrEqualTo(tao.get(START), it.plusDays(1)) } ?: cb.conjunction(),
  )
}

fun occurrenceStatusCodeIn(vararg statusCodes: OccurrenceStatus.Code) = occurrenceStatusCodeIn(statusCodes.toSet())

fun occurrenceStatusCodeIn(statusCodes: Set<OccurrenceStatus.Code>) = Specification<TemporaryAbsenceOccurrence> { taa, _, _ ->
  val status = taa.join<TemporaryAbsenceOccurrence, OccurrenceStatus>(STATUS, JoinType.INNER)
  status.get<String>(CODE).`in`(statusCodes.map { it.name })
}

fun forAuthorisation(authorisationId: UUID) = Specification<TemporaryAbsenceOccurrence> { tao, _, cb ->
  val authorisation =
    tao.join<TemporaryAbsenceOccurrence, TemporaryAbsenceAuthorisation>(AUTHORISATION, JoinType.INNER)
  cb.equal(authorisation.get<UUID>(TemporaryAbsenceAuthorisation.ID), authorisationId)
}

fun startAfter(start: LocalDateTime) = Specification<TemporaryAbsenceOccurrence> { tao, _, cb ->
  cb.greaterThanOrEqualTo(tao.get(START), start)
}

fun startBefore(end: LocalDateTime) = Specification<TemporaryAbsenceOccurrence> { tao, _, cb ->
  cb.lessThan(tao.get(START), end)
}

fun AbsenceCategorisationFilter.matchesOccurrence() = Specification<TemporaryAbsenceOccurrence> { tao, _, _ ->
  val fieldName = when (domainCode) {
    ReferenceDataDomain.Code.ABSENCE_TYPE -> TemporaryAbsenceOccurrence.ABSENCE_TYPE
    ReferenceDataDomain.Code.ABSENCE_SUB_TYPE -> TemporaryAbsenceOccurrence.ABSENCE_SUB_TYPE
    ReferenceDataDomain.Code.ABSENCE_REASON_CATEGORY -> TemporaryAbsenceOccurrence.ABSENCE_REASON_CATEGORY
    ReferenceDataDomain.Code.ABSENCE_REASON -> TemporaryAbsenceOccurrence.ABSENCE_REASON
    else -> throw IllegalArgumentException("Not a valid absence categorisation filter")
  }
  val rd = tao.join<TemporaryAbsenceOccurrence, ReferenceData>(fieldName, JoinType.INNER)
  rd.get<String>(CODE).`in`(codes)
}
