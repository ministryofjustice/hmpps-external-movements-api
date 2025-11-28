package uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence

import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.Join
import jakarta.persistence.criteria.Predicate
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.authorisation.TemporaryAbsenceAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.person.PersonSummary
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.person.PersonSummary.Companion.FIRST_NAME
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.person.PersonSummary.Companion.IDENTIFIER
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.person.PersonSummary.Companion.LAST_NAME

fun Join<TemporaryAbsenceAuthorisation, PersonSummary>.matchesName(cb: CriteriaBuilder, name: String): Predicate {
  val matches = name.replace(",", " ").split("\\s".toRegex())
    .filter { it.isNotBlank() }
    .map {
      cb.or(
        cb.like(cb.lower(this[LAST_NAME]), "%${it.lowercase()}%", '\\'),
        cb.like(cb.lower(this[FIRST_NAME]), "%${it.lowercase()}%", '\\'),
      )
    }.toTypedArray()
  return cb.and(*matches)
}

fun Join<TemporaryAbsenceAuthorisation, PersonSummary>.matchesIdentifier(
  cb: CriteriaBuilder,
  identifier: String,
): Predicate = cb.equal(get<String>(IDENTIFIER), identifier.uppercase())
