package uk.gov.justice.digital.hmpps.externalmovementsapi.model

fun String.removeNullChar() = replace("\u0000", "").trim()
