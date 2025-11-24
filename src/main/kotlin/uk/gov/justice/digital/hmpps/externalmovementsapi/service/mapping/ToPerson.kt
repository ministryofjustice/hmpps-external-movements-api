package uk.gov.justice.digital.hmpps.externalmovementsapi.service.mapping

import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.person.PersonSummary
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.prisonersearch.Prisoner
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.Person

fun Prisoner.asPerson() = Person(prisonerNumber, firstName, lastName, dateOfBirth, cellLocation)
fun PersonSummary.asPerson(): Person = Person(identifier, firstName, lastName, dateOfBirth, cellLocation)
