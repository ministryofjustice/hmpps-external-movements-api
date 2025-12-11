package uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.occurrence

import jakarta.persistence.QueryHint
import jakarta.persistence.criteria.JoinType
import org.hibernate.jpa.HibernateHints
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.jpa.repository.QueryHints
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.authorisation.TemporaryAbsenceAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.authorisation.TemporaryAbsenceAuthorisation.Companion.PERSON
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.authorisation.TemporaryAbsenceAuthorisation.Companion.PRISON_CODE
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.occurrence.TemporaryAbsenceOccurrence.Companion.AUTHORISATION
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.occurrence.TemporaryAbsenceOccurrence.Companion.END
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.occurrence.TemporaryAbsenceOccurrence.Companion.START
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.occurrence.TemporaryAbsenceOccurrence.Companion.STATUS
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.person.PersonSummary
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.person.matchesIdentifier
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.person.matchesName
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceData
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceData.Companion.KEY
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataKey
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataKey.Companion.CODE
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.TapOccurrenceStatus
import uk.gov.justice.digital.hmpps.externalmovementsapi.exception.NotFoundException
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.DateRange
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

interface TemporaryAbsenceOccurrenceRepository :
  JpaRepository<TemporaryAbsenceOccurrence, UUID>,
  JpaSpecificationExecutor<TemporaryAbsenceOccurrence> {
  fun findByAuthorisationPersonIdentifierAndStartAndEnd(
    personIdentifier: String,
    start: LocalDateTime,
    end: LocalDateTime,
  ): TemporaryAbsenceOccurrence?

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

  fun findByAuthorisationId(authorisationId: UUID): List<TemporaryAbsenceOccurrence>
  fun findByAuthorisationIdIn(authorisationIds: Set<UUID>): List<TemporaryAbsenceOccurrence>

  @Query(
    """
      select tao from TemporaryAbsenceOccurrence tao
      where tao.end < :dateTime
        and tao.status.key in :statuses
    """,
  )
  @QueryHints(value = [QueryHint(name = HibernateHints.HINT_NATIVE_LOCK_MODE, value = "UPGRADE-SKIPLOCKED")])
  fun findPastOccurrences(
    statuses: Set<ReferenceDataKey>,
    dateTime: LocalDateTime,
    pageable: Pageable,
  ): List<TemporaryAbsenceOccurrence>

  @Query(
    """
    select
    sum(case when tao.start between current_date and (current_date + 1) then 1 else 0 end) as leavingToday,
    sum(case when tao.start between (current_date + 1) and (current_date + 8) then 1 else 0 end) as leavingNextSevenDays
    from temporary_absence_occurrence tao
        join temporary_absence_authorisation taa on taa.id = tao.authorisation_id
        join reference_data st on st.id = taa.status_id
    where taa.prison_code = :prisonIdentifier
      and st.code = 'APPROVED'
      and tao.start between current_date and (current_date + 8)
    group by taa.prison_code
  """,
    nativeQuery = true,
  )
  fun findUpcomingLeaverCounts(prisonIdentifier: String): PrisonLeaverCounts?

  @Query(
    """
    select count(1) as returningToday
    from temporary_absence_occurrence tao
        join temporary_absence_authorisation taa on taa.id = tao.authorisation_id
        join reference_data st on st.id = taa.status_id
    where taa.prison_code = :prisonIdentifier
      and st.code = 'APPROVED'
      and tao.end between current_date and (current_date + 1)
  """,
    nativeQuery = true,
  )
  fun findReturningTodayCount(prisonIdentifier: String): Int

  @Modifying
  fun deleteByAuthorisationPersonIdentifier(personIdentifier: String)
}

interface PrisonLeaverCounts {
  val leavingToday: Int
  val leavingNextSevenDays: Int

  data object Default : PrisonLeaverCounts {
    override val leavingToday: Int = 0
    override val leavingNextSevenDays: Int = 0
  }
}

fun TemporaryAbsenceOccurrenceRepository.getOccurrence(id: UUID) = findByIdOrNull(id) ?: throw NotFoundException("Temporary absence occurrence not found")

fun occurrenceMatchesPrisonCode(prisonCode: String) = Specification<TemporaryAbsenceOccurrence> { tao, _, cb ->
  val authorisation = tao.join<TemporaryAbsenceOccurrence, TemporaryAbsenceAuthorisation>(AUTHORISATION, JoinType.INNER)
  cb.equal(authorisation.get<String>(PRISON_CODE), prisonCode)
}

fun occurrenceMatchesPersonIdentifier(personIdentifier: String) = Specification<TemporaryAbsenceOccurrence> { tao, _, cb ->
  val authorisation =
    tao.join<TemporaryAbsenceOccurrence, TemporaryAbsenceAuthorisation>(AUTHORISATION, JoinType.INNER)
  authorisation.join<TemporaryAbsenceAuthorisation, PersonSummary>(PERSON, JoinType.INNER).matchesIdentifier(cb, personIdentifier)
}

fun occurrenceMatchesPersonName(name: String) = Specification<TemporaryAbsenceOccurrence> { tao, _, cb ->
  val authorisation =
    tao.join<TemporaryAbsenceOccurrence, TemporaryAbsenceAuthorisation>(AUTHORISATION, JoinType.INNER)
  authorisation.join<TemporaryAbsenceAuthorisation, PersonSummary>(PERSON, JoinType.INNER).matchesName(cb, name)
}

fun occurrenceMatchesDateRange(start: LocalDate?, end: LocalDate?) = Specification<TemporaryAbsenceOccurrence> { tao, _, cb ->
  cb.and(
    start?.let { cb.greaterThanOrEqualTo(tao.get(START), it.atStartOfDay()) } ?: cb.conjunction(),
    end?.let { cb.lessThanOrEqualTo(tao.get(END), it.plusDays(1).atStartOfDay()) } ?: cb.conjunction(),
  )
}

fun occurrenceStatusCodeIn(vararg statusCodes: TapOccurrenceStatus.Code) = occurrenceStatusCodeIn(statusCodes.toSet())

fun occurrenceStatusCodeIn(statusCodes: Set<TapOccurrenceStatus.Code>) = Specification<TemporaryAbsenceOccurrence> { taa, _, _ ->
  val status = taa.join<TemporaryAbsenceOccurrence, ReferenceData>(STATUS, JoinType.INNER)
  status.get<String>(KEY).get<String>(CODE).`in`(statusCodes.map { it.name })
}

fun forAuthorisation(authorisationId: UUID) = Specification<TemporaryAbsenceOccurrence> { tao, _, cb ->
  val authorisation =
    tao.join<TemporaryAbsenceOccurrence, TemporaryAbsenceAuthorisation>(AUTHORISATION, JoinType.INNER)
  cb.equal(authorisation.get<UUID>(TemporaryAbsenceAuthorisation.ID), authorisationId)
}

fun startAfter(start: LocalDate) = Specification<TemporaryAbsenceOccurrence> { tao, _, cb ->
  cb.greaterThanOrEqualTo(tao.get(START), start.atStartOfDay())
}

fun startBefore(end: LocalDate) = Specification<TemporaryAbsenceOccurrence> { tao, _, cb ->
  cb.lessThan(tao.get(START), end)
}
