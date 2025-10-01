package uk.gov.justice.digital.hmpps.externalmovementsapi.service.mapping

import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.prisonersearch.Prisoner
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.Person

fun Prisoner.asPerson() = Person(prisonerNumber, firstName, lastName, dateOfBirth, cellLocation)
