package uk.gov.justice.digital.hmpps.externalmovementsapi.service.mapping

import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.person.PersonSummary
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.Person

fun PersonSummary.asPerson(): Person = Person(identifier, firstName, lastName, prisonCode, cellLocation)
