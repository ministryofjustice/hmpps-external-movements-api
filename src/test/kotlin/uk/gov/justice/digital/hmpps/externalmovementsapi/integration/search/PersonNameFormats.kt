package uk.gov.justice.digital.hmpps.externalmovementsapi.integration.search

import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.person.PersonSummary

fun PersonSummary.nameFormats(): List<String> = listOf(
  "$firstName $lastName",
  "$lastName $firstName",
  "$lastName,$firstName",
  "$lastName, $firstName",
)
