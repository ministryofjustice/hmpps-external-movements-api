package uk.gov.justice.digital.hmpps.externalmovementsapi.model

import java.time.LocalDateTime

data class AtAndBy(val at: LocalDateTime, val by: String, val displayName: String)
